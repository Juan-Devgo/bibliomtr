import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  Scatter,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { PatternTraceResponse } from '@/types/patternSearch'

/** Tamaño fijo en px; Recharts no dibuja dentro de `ResponsiveContainer` en jsdom. */
const CHART_WIDTH = 640
const CHART_HEIGHT = 260
/** Por encima de esta cantidad de puntos la línea se dibuja sin marcadores, para trazas largas. */
const MAX_DOTS = 200

/** Punto del gráfico: x e y tomados de la traza; `marked` indica una colisión de Rabin-Karp. */
interface ChartPoint {
  x: number
  y: number
  marked: boolean
}

/** Datos y textos del gráfico de un algoritmo. */
interface ChartModel {
  label: string
  caption: string
  xLabel: string
  yLabel: string
  points: ChartPoint[]
  /** Hash del patrón en Rabin-Karp (línea de referencia). */
  reference: number | null
}

/**
 * Datos del gráfico de una traza CALCULADO (REQ-27), sin calcular nada nuevo: KMP, x = número de comparación e
 * y = s; Boyer-Moore, x = número de alineación e y = salto aplicado; Rabin-Karp, x = s e y = hash de la ventana,
 * con las colisiones marcadas y hash(P) como referencia. `null` si la respuesta no trae traza.
 *
 * Complejidad: O(c) en tiempo y espacio, con c = tamaño de la traza.
 */
function chartModel(response: PatternTraceResponse): ChartModel | null {
  switch (response.algorithm) {
    case 'kmp': {
      const comparisons = response.trace?.comparisons
      if (!comparisons) {
        return null
      }
      return {
        label: 'Gráfico de s por comparación',
        caption: `Desplazamiento s = i − j de cada una de las ${comparisons.length} comparaciones.`,
        xLabel: 'Comparación',
        yLabel: 's',
        points: comparisons.map((comparison, index) => ({ x: index + 1, y: comparison.shift, marked: false })),
        reference: null,
      }
    }
    case 'boyer-moore': {
      const alignments = response.trace?.alignments
      if (!alignments) {
        return null
      }
      return {
        label: 'Gráfico del salto aplicado por alineación',
        caption: `Salto aplicado en cada una de las ${alignments.length} alineaciones.`,
        xLabel: 'Alineación',
        yLabel: 'Salto aplicado',
        points: alignments.map((alignment, index) => ({ x: index + 1, y: alignment.appliedShift, marked: false })),
        reference: null,
      }
    }
    case 'rabin-karp': {
      const trace = response.trace
      if (!trace) {
        return null
      }
      const collisions = trace.collisions.length === 0 ? 'sin colisiones' : `colisiones en s = ${trace.collisions.join(', ')}`
      return {
        label: 'Gráfico del hash por ventana',
        caption: `Hash t(s) de cada ventana frente a h(P) = ${trace.patternHash}; ${collisions}.`,
        xLabel: 's',
        yLabel: 'Hash',
        points: trace.windows.map((window) => ({
          x: window.shift,
          y: window.hash,
          marked: window.collision,
        })),
        reference: trace.patternHash,
      }
    }
    default:
      return null
  }
}

interface TraceChartProps {
  /** Respuesta de traza CALCULADO de cualquiera de los 3 algoritmos. */
  response: PatternTraceResponse
}

/**
 * Gráfico de la traza (spec 004, REQ-27, AC-16, AC-17, AC-18): en KMP el desplazamiento s de cada comparación,
 * en Boyer-Moore el salto aplicado por alineación (barras) y en Rabin-Karp el hash de cada ventana con una línea
 * de referencia en hash(P) y las colisiones marcadas. Recharts solo dibuja los valores de la traza (constitución
 * P2); usa tamaño fijo y sin animación para que exista en el DOM de inmediato.
 *
 * Complejidad por render: O(c) en tiempo y espacio, con c = tamaño de la traza.
 */
function TraceChart({ response }: TraceChartProps) {
  const model = chartModel(response)
  if (!model) {
    return null
  }
  const showDots = model.points.length <= MAX_DOTS
  // Las colisiones van en una serie aparte, con sus propios puntos, para marcar solo esas ventanas.
  const marked = model.points.filter((point) => point.marked)

  return (
    <figure aria-label={model.label} style={{ margin: 0 }}>
      <ComposedChart
        width={CHART_WIDTH}
        height={CHART_HEIGHT}
        data={model.points}
        margin={{ top: 16, right: 24, bottom: 24, left: 8 }}
      >
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="x"
          type="number"
          domain={['dataMin', 'dataMax']}
          allowDecimals={false}
          label={{ value: model.xLabel, position: 'insideBottom', offset: -12 }}
        />
        <YAxis allowDecimals={false} label={{ value: model.yLabel, angle: -90, position: 'insideLeft' }} />
        <Tooltip />
        <Legend verticalAlign="top" />
        {response.algorithm === 'boyer-moore' ? (
          <Bar dataKey="y" name={model.yLabel} fill="#1976d2" isAnimationActive={false} />
        ) : (
          <Line
            dataKey="y"
            name={model.yLabel}
            type={response.algorithm === 'kmp' ? 'stepAfter' : 'linear'}
            stroke="#1976d2"
            dot={showDots}
            isAnimationActive={false}
          />
        )}
        {model.reference !== null && (
          <ReferenceLine y={model.reference} stroke="#2e7d32" strokeDasharray="6 3" label={`h(P) = ${model.reference}`} />
        )}
        {response.algorithm === 'rabin-karp' && (
          <Scatter
            data={marked}
            dataKey="y"
            name="Colisión"
            fill="#d32f2f"
            shape="cross"
            isAnimationActive={false}
          />
        )}
      </ComposedChart>
      <figcaption style={{ fontSize: '0.8rem' }}>{model.caption}</figcaption>
    </figure>
  )
}

export default TraceChart
