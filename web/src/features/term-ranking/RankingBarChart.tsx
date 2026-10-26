import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import type { TermFrequency } from '@/types/ranking'

/** Ancho fijo del gráfico en px; Recharts no dibuja dentro de `ResponsiveContainer` en jsdom. */
const CHART_WIDTH = 520
/** Alto por barra y margen fijo, para que el gráfico crezca con N. */
const BAR_HEIGHT = 26
const CHART_PADDING = 48

interface RankingBarChartProps {
  /** Top N en el orden del ranking. */
  top: readonly TermFrequency[]
  /** Nombre accesible del gráfico. */
  label: string
}

/**
 * Gráfico de barras horizontales del top N de un algoritmo (REQ-5, AC-5): una barra por término, de
 * longitud igual a su frecuencia, en el orden del ranking (el primero arriba). Recharts solo dibuja
 * (constitución P2); el orden lo da el backend. Usa `width`/`height` explícitos y sin animación para
 * que las barras existan en el DOM de inmediato.
 *
 * Complejidad por render: O(N) en tiempo y espacio.
 */
function RankingBarChart({ top, label }: RankingBarChartProps) {
  const height = CHART_PADDING + BAR_HEIGHT * Math.max(top.length, 1)
  return (
    <figure aria-label={label} style={{ margin: 0 }}>
      <BarChart
        width={CHART_WIDTH}
        height={height}
        data={[...top]}
        layout="vertical"
        margin={{ top: 8, right: 16, bottom: 8, left: 16 }}
      >
        <CartesianGrid strokeDasharray="3 3" horizontal={false} />
        <XAxis type="number" dataKey="frequency" allowDecimals={false} />
        <YAxis type="category" dataKey="term" width={110} interval={0} />
        <Tooltip />
        <Bar dataKey="frequency" name="Frecuencia" fill="#1976d2" isAnimationActive={false} />
      </BarChart>
    </figure>
  )
}

export default RankingBarChart
