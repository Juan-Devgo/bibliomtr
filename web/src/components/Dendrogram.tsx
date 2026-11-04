import Box from '@mui/material/Box'
import { cluster, hierarchy } from 'd3'
import { useMemo } from 'react'
import { toHierarchy, type DendrogramNode } from '@/features/clustering/dendrogramTree'
import type { Merge } from '@/types/clustering'
import { createHeightScale, DENDROGRAM_WIDTH, heightTicks } from './dendrogramScale'

/** Alto de la fila de cada hoja. */
const ROW_HEIGHT = 30
/** Margen superior. */
const TOP_MARGIN = 8
/** Alto del eje de alturas y su título. */
const AXIS_HEIGHT = 44

interface DendrogramProps {
  /** N − 1 fusiones que devolvió la API, estilo SciPy. */
  merges: readonly Merge[]
  /** Etiqueta visible de cada hoja, en el orden de los artículos (p. ej. `#3 Título recortado`). */
  labels: readonly string[]
  /** Texto completo de cada hoja para el `<title>` (p. ej. el título sin recortar); por defecto, `labels`. */
  titles?: readonly string[]
  /** Nombre accesible del SVG. */
  ariaLabel: string
  /** Altura máxima del eje; los dendrogramas que se comparan deben recibir la misma. Por defecto, 1. */
  maxHeight?: number
}

/** Posición de un nodo en el lienzo. */
interface PositionedNode {
  node: DendrogramNode
  x: number
  y: number
}

/** Enlace en codo de un hijo a su padre. */
interface Link {
  key: string
  path: string
}

/**
 * Dendrograma horizontal dibujado con D3 (spec 003, REQ-14, REQ-15).
 *
 * D3 solo ubica los nodos: `hierarchy` + `cluster` reparten las hojas en filas, en el orden de los hijos que da
 * la API, y `scaleLinear` lleva la altura de cada fusión, en [0, `maxHeight`], al eje horizontal. No calcula fusiones,
 * alturas, cortes ni grupos, ni reordena las hojas. Cada hoja muestra su etiqueta y, al pasar el cursor, el
 * título completo; cada fusión, su grupo, altura y tamaño.
 *
 * Complejidad por render: O(N) en tiempo y espacio, con N hojas.
 */
function Dendrogram({ merges, labels, titles = labels, ariaLabel, maxHeight = 1 }: DendrogramProps) {
  const heightScale = useMemo(() => createHeightScale(maxHeight), [maxHeight])
  const ticks = useMemo(() => heightTicks(maxHeight), [maxHeight])
  const layout = useMemo(() => {
    const root = hierarchy(toHierarchy(merges, labels))
    const n = labels.length
    // Hojas equiespaciadas: separación 1 entre cualquier par vecino.
    cluster<DendrogramNode>()
      .size([(n - 1) * ROW_HEIGHT, 1])
      .separation(() => 1)(root)
    const positioned = new Map<number, PositionedNode>()
    for (const node of root.descendants()) {
      positioned.set(node.data.id, {
        node: node.data,
        x: heightScale(node.data.height),
        y: TOP_MARGIN + ROW_HEIGHT / 2 + (node.x ?? 0),
      })
    }
    const links: Link[] = []
    for (const node of root.descendants()) {
      if (node.parent) {
        const child = positioned.get(node.data.id)
        const parent = positioned.get(node.parent.data.id)
        if (child && parent) {
          links.push({ key: `${node.data.id}-${node.parent.data.id}`, path: `M${child.x},${child.y}H${parent.x}V${parent.y}` })
        }
      }
    }
    return { positioned: [...positioned.values()], links, plotBottom: TOP_MARGIN + n * ROW_HEIGHT }
  }, [merges, labels, heightScale])

  const height = layout.plotBottom + AXIS_HEIGHT
  const axisY = layout.plotBottom + 4

  return (
    <Box sx={{ color: 'text.primary', width: '100%' }}>
      <svg
        role="img"
        aria-label={ariaLabel}
        viewBox={`0 0 ${DENDROGRAM_WIDTH} ${height}`}
        width="100%"
        style={{ display: 'block', fontSize: 13 }}
      >
        <g fill="none" stroke="currentColor" strokeWidth={2}>
          {layout.links.map((link) => (
            <path key={link.key} d={link.path} />
          ))}
        </g>
        {layout.positioned.map(({ node, x, y }) =>
          node.children ? (
            <circle key={node.id} cx={x} cy={y} r={4} fill="currentColor" data-height={node.height}>
              <title>{`G${node.id}: altura ${node.height.toFixed(3)}, ${node.size} artículos`}</title>
            </circle>
          ) : (
            <g key={node.id}>
              <title>{titles[node.id]}</title>
              <text x={x - 6} y={y} textAnchor="end" dominantBaseline="middle" fill="currentColor">
                {labels[node.id]}
              </text>
            </g>
          ),
        )}
        <g aria-label="Eje de altura" stroke="currentColor" fill="currentColor">
          <line x1={heightScale(0)} x2={heightScale(maxHeight)} y1={axisY} y2={axisY} />
          {ticks.map((tick) => (
            <g key={tick}>
              <line x1={heightScale(tick)} x2={heightScale(tick)} y1={axisY} y2={axisY + 4} />
              <text x={heightScale(tick)} y={axisY + 16} textAnchor="middle" stroke="none">
                {String(tick)}
              </text>
            </g>
          ))}
          <text x={(heightScale(0) + heightScale(maxHeight)) / 2} y={axisY + 34} textAnchor="middle" stroke="none">
            Altura (distancia)
          </text>
        </g>
      </svg>
    </Box>
  )
}

export default Dendrogram
