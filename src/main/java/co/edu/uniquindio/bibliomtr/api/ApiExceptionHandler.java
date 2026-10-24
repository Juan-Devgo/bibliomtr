package co.edu.uniquindio.bibliomtr.api;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import co.edu.uniquindio.bibliomtr.api.benchmark.BenchmarkInProgressException;
import co.edu.uniquindio.bibliomtr.api.similarity.ArticleSelectionValidator;

/**
 * Traduce los errores de la API a respuestas {@link ProblemDetail} (RFC 9457) con el mensaje en español en
 * {@code detail} (spec 002: REQ-4, AC-3; REQ-19, AC-14. Spec 001: REQ-15, AC-14).
 *
 * <p><b>Casos.</b>
 * <ul>
 *   <li>Parámetro no entero ({@code n=abc}): {@link MethodArgumentTypeMismatchException} → 400.</li>
 *   <li>Parámetro menor que 1 ({@code @Min(1)}): {@link HandlerMethodValidationException} → 400.</li>
 *   <li>Algoritmo desconocido u otro argumento inválido de la capa de algoritmos:
 *       {@link IllegalArgumentException} → 400 con su mensaje, que ya está en español
 *       ({@code SortAlgorithm.fromSlug}, {@code ComparisonAlgorithm.fromSlug}). Incluye la selección
 *       inválida de artículos ({@code InvalidSelectionException}).</li>
 *   <li>Parámetro obligatorio ausente, p. ej. {@code b} en {@code GET /api/similarity/trace}:
 *       {@link MissingServletRequestParameterException} → 400 con "Falta el parámetro &lt;nombre&gt;".</li>
 *   <li>Cuerpo ausente o que no es JSON con una lista de enteros:
 *       {@link HttpMessageNotReadableException} → 400 con "El cuerpo debe ser un JSON con articleIds: lista
 *       de enteros" (el único cuerpo JSON de la API es el de {@code POST /api/similarity/comparisons}).</li>
 *   <li>Medición JMH pedida mientras otra está EN_CURSO: {@link BenchmarkInProgressException} → 409 con
 *       "Ya hay una medición en curso".</li>
 * </ul>
 * Los parámetros validados de la API ({@code n}, {@code bucketSize}) son enteros mayores o iguales a 1, de
 * ahí el mensaje "El parámetro &lt;nombre&gt; debe ser un entero mayor o igual a 1".
 *
 * <p>Complejidad: O(1) en tiempo y espacio por error (O(p) en el caso de validación, con p = parámetros
 * inválidos, para tomar el primero).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final String INTEGER_PARAMETER_MESSAGE = "El parámetro %s debe ser un entero mayor o igual a 1";

	/**
	 * Parámetro que no se puede convertir a entero, p. ej. {@code n=abc}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param exception error de conversión de Spring MVC
	 * @return 400 con el nombre del parámetro
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return badRequest(INTEGER_PARAMETER_MESSAGE.formatted(exception.getName()));
	}

	/**
	 * Parámetro que no cumple {@code @Min(1)}, p. ej. {@code n=0}.
	 *
	 * <p>Complejidad: O(p) en tiempo y O(1) en espacio, con p = número de parámetros inválidos.
	 *
	 * @param exception error de la validación de métodos de Spring MVC
	 * @return 400 con el nombre del primer parámetro inválido
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleMethodValidation(HandlerMethodValidationException exception) {
		List<ParameterValidationResult> results = exception.getParameterValidationResults();
		String name = results.isEmpty() ? "recibido" : parameterName(results.getFirst().getMethodParameter());
		return badRequest(INTEGER_PARAMETER_MESSAGE.formatted(name));
	}

	/**
	 * Argumento inválido rechazado por la capa de algoritmos, como un slug de algoritmo desconocido
	 * ("Algoritmo desconocido: &lt;x&gt;. Valores válidos: counting, radix, bucket").
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param exception error con el mensaje en español
	 * @return 400 con ese mensaje
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return badRequest(exception.getMessage());
	}

	/**
	 * Parámetro obligatorio ausente (spec 001, REQ-15).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param exception error de Spring MVC con el nombre del parámetro
	 * @return 400 con "Falta el parámetro &lt;nombre&gt;"
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
		return badRequest("Falta el parámetro " + exception.getParameterName());
	}

	/**
	 * Cuerpo ausente o ilegible, p. ej. sin cuerpo o con {@code {"articleIds": ["x"]}} (spec 001, REQ-15,
	 * AC-14).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param exception error de lectura del cuerpo
	 * @return 400 con "El cuerpo debe ser un JSON con articleIds: lista de enteros"
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
		return badRequest(ArticleSelectionValidator.INVALID_BODY_MESSAGE);
	}

	/**
	 * Medición JMH pedida mientras otra está EN_CURSO (REQ-19, AC-14).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param exception rechazo del servicio de mediciones
	 * @return 409 con "Ya hay una medición en curso"
	 */
	@ExceptionHandler(BenchmarkInProgressException.class)
	public ProblemDetail handleBenchmarkInProgress(BenchmarkInProgressException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Conflicto");
		return problem;
	}

	/**
	 * Nombre del parámetro en la URL: el de {@code @RequestParam}/{@code @PathVariable} si está declarado, o
	 * el del método en otro caso.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static String parameterName(MethodParameter parameter) {
		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		if (requestParam != null && !requestParam.name().isEmpty()) {
			return requestParam.name();
		}
		PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
		if (pathVariable != null && !pathVariable.name().isEmpty()) {
			return pathVariable.name();
		}
		String name = parameter.getParameterName();
		return name != null ? name : "recibido";
	}

	/**
	 * Construye un 400 con el título y el detalle en español.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static ProblemDetail badRequest(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Solicitud inválida");
		return problem;
	}

}
