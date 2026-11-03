/**
 * Fórmulas en LaTeX de la traza de la búsqueda de patrones (spec 004, REQ-26); KaTeX solo las dibuja
 * (constitución P2, P6). Son las mismas de la "Definición formal" del Javadoc de cada stub
 * (`KnuthMorrisPratt`, `BoyerMoore`, `RabinKarp`) y de la guía § 11.
 */

/** KMP: función de prefijo π (longitud del prefijo propio más largo de P[0..i] que también es sufijo). */
export const KMP_PREFIX_FORMULA = String.raw`\pi[i] = \max\{\, k : k < i + 1 \ \wedge\ P[0..k-1] = P[i-k+1..i] \,\}, \qquad \pi[0] = 0`

/** KMP: desplazamiento de cada comparación, retroceso tras un desajuste y tras una ocurrencia. */
export const KMP_SEARCH_FORMULA = String.raw`\begin{gathered}
s = i - j \\
T[i] \ne P[j] \ \wedge\ j > 0 \;\Rightarrow\; j \leftarrow \pi[j - 1] \\
j = m \;\Rightarrow\; \text{ocurrencia en } s = i - m + 1,\quad j \leftarrow \pi[m - 1]
\end{gathered}`

/** Boyer-Moore: regla del carácter malo (un carácter ausente vale −1; el salto puede ser ≤ 0). */
export const BM_BAD_CHARACTER_FORMULA = String.raw`\operatorname{last}(c) = \max\{\, k : P[k] = c \,\}\ \ (-1 \text{ si } c \notin P), \qquad \text{salto}_{cm}(j) = j - \operatorname{last}(T[s + j])`

/** Boyer-Moore: regla fuerte del sufijo bueno, indexada por la posición j del desajuste, y borde de P. */
export const BM_GOOD_SUFFIX_FORMULA = String.raw`\begin{gathered}
\text{salto}_{sb}(j) = \min\{\, t > 0 : P[j+1..m-1] \text{ coincide desplazado } t \ \wedge\ P[j - t] \ne P[j] \,\} \\
\text{(si no hay, el borde más largo de } P \text{ que encaja como prefijo)}, \qquad
\operatorname{borde}(P) = \max\{\, k < m : P[0..k-1] = P[m-k..m-1] \,\}
\end{gathered}`

/** Boyer-Moore: desplazamiento aplicado tras un desajuste y tras una ocurrencia completa. */
export const BM_SHIFT_FORMULA = String.raw`\begin{gathered}
s \leftarrow s + \max\big(\text{salto}_{cm}(j),\ \text{salto}_{sb}(j),\ 1\big) \\
\text{tras una ocurrencia: } s \leftarrow s + \big(m - \operatorname{borde}(P)\big)
\end{gathered}`

/** Rabin-Karp: hash polinomial modular y factor h, con d = 65 536 y q primo. */
export const RK_HASH_FORMULA = String.raw`h(S) = \Big(\sum_{i=0}^{m-1} S[i] \cdot d^{\,m-1-i}\Big) \bmod q, \qquad h = d^{\,m-1} \bmod q`

/** Rabin-Karp: rolling hash de la ventana siguiente, llevado a [0, q), y verificación de las coincidencias. */
export const RK_ROLLING_FORMULA = String.raw`\begin{gathered}
t(s+1) = \big(d \cdot (t(s) - T[s] \cdot h) + T[s+m]\big) \bmod q \\
t(s) = h(P) \;\Rightarrow\; \text{verificar } P = T[s..s+m-1] \text{: si no, colisión}
\end{gathered}`
