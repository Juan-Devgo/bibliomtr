import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { TfIdfCosineTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal } from './traceFormat'

interface TfIdfCosineTraceViewProps {
  trace: TfIdfCosineTrace
  /** Coseno en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/**
 * Traza del coseno TF-IDF paso a paso (REQ-16, REQ-17, AC-15): vocabulario, df e idf, tf, pesos, producto
 * punto y normas, y coseno, cada paso con su fórmula en KaTeX (constitución P6, P7). Todos los arreglos van
 * alineados con el vocabulario, así cada elemento se muestra como `término: valor`.
 *
 * Complejidad por render: O(min(|V|, 50)), con V = vocabulario de los dos abstracts.
 */
function TfIdfCosineTraceView({ trace, similarity }: TfIdfCosineTraceViewProps) {
  const { vocabulary } = trace
  // Dibuja el término t_i con el valor i-ésimo de un arreglo alineado con el vocabulario.
  const byTerm = (values: readonly number[]) => (term: string, index: number) => `${term}: ${decimal(values[index])}`

  return (
    <StepList>
      <TraceStep
        number={1}
        title="Vocabulario"
        formula={String.raw`V = \operatorname{tokens}(a) \cup \operatorname{tokens}(b),\quad |V| = ${vocabulary.length},\quad N = ${trace.documentCount}`}
      >
        <Typography variant="body2" color="text.secondary">
          N es el número de abstracts seleccionables del corpus, sobre los que se calcula el IDF.
        </Typography>
        <PagedArray items={vocabulary} renderItem={(term) => term} label="Vocabulario V" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Frecuencia de documento e IDF"
        formula={String.raw`\operatorname{df}(t) = \left|\{\, d : t \in d \,\}\right|,\qquad \operatorname{idf}(t) = \ln\frac{N}{\operatorname{df}(t)}`}
      >
        <PagedArray
          items={vocabulary}
          renderItem={(term, index) => `${term}: df = ${trace.documentFrequency[index]}, idf = ${decimal(trace.idf[index])}`}
          label="df e idf"
        />
      </TraceStep>
      <TraceStep
        number={3}
        title="Frecuencia de término"
        formula={String.raw`\operatorname{tf}(t, d) = \frac{f_{t,d}}{|d|}`}
      >
        <PagedArray items={vocabulary} renderItem={byTerm(trace.tfA)} label="tf en a" />
        <PagedArray items={vocabulary} renderItem={byTerm(trace.tfB)} label="tf en b" />
      </TraceStep>
      <TraceStep
        number={4}
        title="Pesos"
        formula={String.raw`w_{t,d} = \operatorname{tf}(t, d)\cdot\operatorname{idf}(t)`}
      >
        <PagedArray items={vocabulary} renderItem={byTerm(trace.weightsA)} label="Pesos de a" />
        <PagedArray items={vocabulary} renderItem={byTerm(trace.weightsB)} label="Pesos de b" />
      </TraceStep>
      <TraceStep
        number={5}
        title="Producto punto y normas"
        formula={String.raw`w_a \cdot w_b = \sum_{t \in V} w_{t,a}\,w_{t,b} = ${decimal(trace.dotProduct)},\quad \lVert w_a \rVert = ${decimal(trace.normA)},\quad \lVert w_b \rVert = ${decimal(trace.normB)}`}
      >
        <Typography variant="body2" color="text.secondary">
          Normas euclidianas de los vectores de pesos.
        </Typography>
      </TraceStep>
      <TraceStep
        number={6}
        title="Coseno"
        formula={String.raw`\cos(a, b) = \frac{w_a \cdot w_b}{\lVert w_a \rVert\,\lVert w_b \rVert} = ${decimal(similarity)}`}
      >
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default TfIdfCosineTraceView
