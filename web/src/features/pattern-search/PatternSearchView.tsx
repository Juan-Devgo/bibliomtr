import Alert from '@mui/material/Alert'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { usePatternSearch, type PatternSearchParams } from '@/api/usePatternSearch'
import type { PatternSearchResponse, StringMatchingSlug } from '@/types/patternSearch'
import AlgorithmSelector, { defaultAlgorithm } from './AlgorithmSelector'
import HighlightedAbstract from './HighlightedAbstract'
import PatternSearchForm from './PatternSearchForm'
import PatternSearchResults from './PatternSearchResults'
import PatternTraceView from './PatternTraceView'

/**
 * Algoritmo resaltado: el elegido si sigue CALCULADO en la respuesta; si no, el de `defaultAlgorithm`.
 *
 * Complejidad: O(1) (3 algoritmos).
 */
function highlightedAlgorithm(
  response: PatternSearchResponse,
  chosen: StringMatchingSlug | null,
): StringMatchingSlug | null {
  const result = response.algorithms.find((candidate) => candidate.algorithm === chosen)
  return result && result.status === 'CALCULADO' ? result.algorithm : defaultAlgorithm(response)
}

/**
 * Abstract del artículo elegido con las posiciones del algoritmo del selector (REQ-20, AC-15) y la traza de ese
 * algoritmo sobre ese abstract, con el patrón y el q de la búsqueda (REQ-22, REQ-26).
 */
function ArticleHighlight({
  response,
  articleId,
  chosen,
  onChoose,
}: {
  response: PatternSearchResponse
  articleId: number
  chosen: StringMatchingSlug | null
  onChoose: (algorithm: StringMatchingSlug) => void
}) {
  const article = response.articles.find((candidate) => candidate.articleId === articleId)
  if (!article) {
    return null
  }
  const algorithm = highlightedAlgorithm(response, chosen)
  const result = response.algorithms.find((candidate) => candidate.algorithm === algorithm)
  if (algorithm === null || !result) {
    return (
      <Alert severity="info">
        Ningún algoritmo está implementado todavía: no hay posiciones para resaltar en el abstract.
      </Alert>
    )
  }
  const positions = result.articles.find((candidate) => candidate.articleId === articleId)?.positions ?? []

  return (
    <Stack spacing={2}>
      <AlgorithmSelector response={response} value={algorithm} onChange={onChoose} />
      <HighlightedAbstract
        title={`#${article.articleId} ${article.title}`}
        text={article.abstractText}
        positions={positions}
        patternLength={response.pattern.length}
        algorithmLabel={result.label}
      />
      <PatternTraceView
        algorithm={algorithm}
        articleId={article.articleId}
        pattern={response.pattern}
        q={String(response.q)}
      />
    </Stack>
  )
}

/**
 * Vista de la pestaña "Búsqueda Patrones" (spec 004, REQ-19, REQ-20, REQ-21).
 *
 * El formulario envía el patrón y q; los resultados muestran por algoritmo los tiempos, el total y la tabla de
 * ocurrencias por artículo. Al elegir una fila se muestra el abstract de ese artículo con las posiciones del
 * algoritmo del selector (por defecto el primer implementado en el orden KMP, Boyer-Moore, Rabin-Karp) y, debajo,
 * la traza paso a paso de ese algoritmo sobre ese abstract (`PatternTraceView`). Cada búsqueda nueva limpia el
 * artículo elegido, porque el resultado cambia; el algoritmo elegido se conserva si sigue
 * implementado. La vista no busca ni compara el patrón con el texto: solo dibuja las posiciones de la API.
 *
 * Complejidad por render: la de `PatternSearchResults` más O(n + k) del abstract elegido.
 */
function PatternSearchView() {
  const [params, setParams] = useState<PatternSearchParams | null>(null)
  const [articleId, setArticleId] = useState<number | null>(null)
  const [chosen, setChosen] = useState<StringMatchingSlug | null>(null)
  const { data, isFetching, error } = usePatternSearch(params)

  const submit = (next: PatternSearchParams) => {
    setParams(next)
    setArticleId(null)
  }

  return (
    <Stack component="section" aria-label="Búsqueda de patrones" spacing={2}>
      <PatternSearchForm onSubmit={submit} busy={isFetching} />
      <PatternSearchResults
        response={isFetching ? undefined : data}
        error={error}
        loading={isFetching}
        selectedArticleId={articleId}
        onSelectArticle={setArticleId}
      />
      {data && !isFetching && !error && articleId !== null && (
        <>
          <Typography variant="h6" component="h2">
            Ocurrencias en el abstract
          </Typography>
          <ArticleHighlight response={data} articleId={articleId} chosen={chosen} onChoose={setChosen} />
        </>
      )}
    </Stack>
  )
}

export default PatternSearchView
