package co.edu.uniquindio.bibliomtr.evaluation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Ejecuta JMH como proceso hijo {@code java -jar benchmarks.jar} con los parámetros fijos de REQ-18 (REQ-17,
 * REQ-21).
 *
 * <p><b>Qué hace.</b>
 * <ol>
 *   <li>Crea un directorio temporal {@code bibliomtr-jmh-*} y escribe la lista en {@code input.tsv}
 *       ({@link FrequencyListFile}).</li>
 *   <li>Lanza {@code <java.home>/bin/java -jar <jar> SortingBenchmark -f 1 -wi 5 -w 1s -i 10 -r 1s -bm avgt
 *       -tu ms -rf json -rff <tmp>/result.json -p inputFile=<tmp>/input.tsv}, con la salida en
 *       {@code jmh.log}. Los valores salen de {@link JmhParameters#FIXED}.</li>
 *   <li>Espera como máximo {@code timeout}. Si vence, termina a la fuerza el proceso y sus descendientes (la
 *       JVM que JMH bifurca con {@code -f 1}) y lanza {@link BenchmarkTimeoutException}.</li>
 *   <li>Si el código de salida no es 0, lanza {@link IllegalStateException} con las últimas líneas de la
 *       salida; si es 0, lee {@code result.json} con {@link JmhResultParser}.</li>
 *   <li>Borra el directorio temporal en todos los casos.</li>
 * </ol>
 *
 * <p><b>Por qué un proceso hijo.</b> El fork de JMH no funciona dentro del fat jar de Spring Boot, y así los
 * benchmarks quedan en {@code src/jmh}, separados de la aplicación (plan, decisiones).
 */
public final class JmhProcessLauncher implements BenchmarkLauncher {

	/** Clase de benchmarks que se ejecuta (expresión regular de JMH). */
	static final String BENCHMARK = "SortingBenchmark";

	private static final int OUTPUT_TAIL_LINES = 10;
	private static final long DESTROY_WAIT_SECONDS = 10;

	private final Path javaExecutable;
	private final Path jar;
	private final JmhResultParser parser = new JmhResultParser();

	/**
	 * Launcher que usa el mismo {@code java} que corre la aplicación ({@code java.home}).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param jar {@code benchmarks.jar} generado por {@code ./gradlew jmhJar}
	 */
	public JmhProcessLauncher(Path jar) {
		this(Path.of(System.getProperty("java.home"), "bin", "java"), jar);
	}

	/**
	 * Launcher con un ejecutable de Java explícito.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param javaExecutable ruta del ejecutable {@code java}
	 * @param jar            {@code benchmarks.jar} generado por {@code ./gradlew jmhJar}
	 */
	public JmhProcessLauncher(Path javaExecutable, Path jar) {
		this.javaExecutable = Objects.requireNonNull(javaExecutable, "El ejecutable de Java no puede ser nulo");
		this.jar = Objects.requireNonNull(jar, "El jar de benchmarks no puede ser nulo");
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio para escribir la lista, más la duración de JMH.
	 */
	@Override
	public List<JmhScore> run(List<TermFrequency> input, Duration timeout) throws BenchmarkTimeoutException {
		Objects.requireNonNull(input, "La lista de frecuencias no puede ser nula");
		Objects.requireNonNull(timeout, "El límite de tiempo no puede ser nulo");
		Path directory = createTempDirectory();
		try {
			Path inputFile = directory.resolve("input.tsv");
			Path resultFile = directory.resolve("result.json");
			Path log = directory.resolve("jmh.log");
			FrequencyListFile.write(inputFile, input);

			Process process = start(command(inputFile, resultFile), log);
			if (!waitFor(process, timeout)) {
				destroyTree(process);
				throw new BenchmarkTimeoutException(timeout);
			}
			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new IllegalStateException("JMH terminó con código %d. Últimas líneas de la salida:%n%s"
						.formatted(exitCode, tail(log)));
			}
			return parser.parse(resultFile);
		} finally {
			deleteRecursively(directory);
		}
	}

	/**
	 * Argumentos fijos del proceso hijo, tomados de {@link JmhParameters#FIXED} (REQ-18). {@code bucketSize} y
	 * la traza desactivada están fijos en el propio {@code SortingBenchmark}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param inputFile  TSV con la lista de frecuencias
	 * @param resultFile JSON donde JMH escribe los resultados
	 * @return comando completo, empezando por el ejecutable de Java
	 */
	List<String> command(Path inputFile, Path resultFile) {
		JmhParameters parameters = JmhParameters.FIXED;
		return List.of(
				javaExecutable.toString(), "-jar", jar.toString(), BENCHMARK,
				"-f", Integer.toString(parameters.forks()),
				"-wi", Integer.toString(parameters.warmupIterations()),
				"-w", parameters.warmupSeconds() + "s",
				"-i", Integer.toString(parameters.measurementIterations()),
				"-r", parameters.measurementSeconds() + "s",
				"-bm", parameters.mode(),
				"-tu", parameters.timeUnit(),
				"-rf", "json",
				"-rff", resultFile.toString(),
				"-p", "inputFile=" + inputFile);
	}

	/**
	 * Lanza el proceso con la salida estándar y de error juntas en {@code log}; escribir a un archivo evita que
	 * el hijo se bloquee con el búfer de la tubería lleno.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio, sin contar el arranque del proceso.
	 */
	private static Process start(List<String> command, Path log) {
		try {
			return new ProcessBuilder(command)
					.redirectErrorStream(true)
					.redirectOutput(log.toFile())
					.start();
		} catch (IOException exception) {
			throw new IllegalStateException("no se pudo lanzar JMH (" + exception.getMessage() + ")", exception);
		}
	}

	/**
	 * Espera el fin del proceso hasta {@code timeout}. Si el hilo se interrumpe (p. ej. al cerrar la
	 * aplicación), termina el proceso y restaura la marca de interrupción.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio, más la espera.
	 *
	 * @return {@code true} si el proceso terminó dentro del límite
	 */
	private static boolean waitFor(Process process, Duration timeout) {
		try {
			return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			destroyTree(process);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("la medición fue interrumpida", exception);
		}
	}

	/**
	 * Termina a la fuerza el proceso y sus descendientes. Los descendientes se recorren antes de matar al padre,
	 * porque al morir este dejan de figurar como sus hijos. Luego espera a que todos terminen, para que el
	 * directorio temporal se pueda borrar y no queden JVM midiendo en segundo plano.
	 *
	 * <p>Complejidad: O(h) en tiempo y espacio, con h = número de procesos descendientes.
	 */
	private static void destroyTree(Process process) {
		List<ProcessHandle> descendants = process.descendants().toList();
		for (ProcessHandle descendant : descendants) {
			descendant.destroyForcibly();
		}
		process.destroyForcibly();
		try {
			process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS);
			for (ProcessHandle descendant : descendants) {
				descendant.onExit().get(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException exception) {
			// Un descendiente que no termina a tiempo no cambia el resultado: la medición ya es FALLIDA.
		}
	}

	/**
	 * Últimas {@value #OUTPUT_TAIL_LINES} líneas de la salida de JMH, para el mensaje de la medición FALLIDA.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = tamaño de la salida.
	 */
	private static String tail(Path log) {
		try {
			List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
			return String.join(System.lineSeparator(),
					lines.subList(Math.max(0, lines.size() - OUTPUT_TAIL_LINES), lines.size()));
		} catch (IOException exception) {
			return "(sin salida)";
		}
	}

	/**
	 * Crea el directorio temporal de la medición.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static Path createTempDirectory() {
		try {
			return Files.createTempDirectory("bibliomtr-jmh-");
		} catch (IOException exception) {
			throw new UncheckedIOException("no se pudo crear el directorio temporal de JMH", exception);
		}
	}

	/**
	 * Borra el directorio temporal y su contenido; los fallos se ignoran porque no afectan el resultado.
	 *
	 * <p>Complejidad: O(f) en tiempo y espacio, con f = número de archivos.
	 */
	private static void deleteRecursively(Path directory) {
		// Se recolectan los caminos y se borran del último al primero: los archivos antes que su directorio,
		// porque Files.walk recorre en preorden.
		List<Path> paths;
		try (Stream<Path> walk = Files.walk(directory)) {
			paths = walk.toList();
		} catch (IOException exception) {
			return;
		}
		for (int i = paths.size() - 1; i >= 0; i--) {
			try {
				Files.deleteIfExists(paths.get(i));
			} catch (IOException exception) {
				// Archivo temporal: se ignora.
			}
		}
	}

}
