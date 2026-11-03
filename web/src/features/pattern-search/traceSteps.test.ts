import { describe, expect, it } from 'vitest'
import {
  BOYER_MOORE_TRACE_ABAB,
  KMP_TRACE_ABAB,
  PENDING_BOYER_MOORE_TRACE,
  RABIN_KARP_COLLISION_TRACE,
  RABIN_KARP_TRACE_ABAB,
} from '@/test/patternSearchFixtures'
import { toFrames } from './traceSteps'

describe('toFrames (REQ-26, AC-16, AC-17, AC-18)', () => {
  it('KMP: un frame por comparación con el s de la traza y el s de la siguiente (AC-16)', () => {
    const frames = toFrames(KMP_TRACE_ABAB)

    expect(frames.map((frame) => frame.shift)).toEqual([0, 0, 0, 0, 2, 2, 4, 6, 7])
    expect(frames.map((frame) => frame.nextShift)).toEqual([0, 0, 0, 2, 2, 4, 6, 7, undefined])
    expect(frames.map((frame) => frame.occurrence)).toEqual([false, false, false, true, false, true, false, false, false])
    // Cada frame trae solo la comparación T[i] vs P[j] de ese paso.
    expect(frames[4].compared).toEqual([{ textIndex: 4, patternIndex: 2, matched: true }])
    expect(frames[6].compared).toEqual([{ textIndex: 6, patternIndex: 2, matched: false }])
    expect(frames.map((frame) => frame.mismatchIndex)).toEqual([
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      2,
      0,
      undefined,
    ])
  })

  it('KMP: la etiqueta trae la comparación, la ocurrencia y el retroceso de la traza', () => {
    const frames = toFrames(KMP_TRACE_ABAB)

    expect(frames[0].label).toBe("Comparación 1: T[0] = 'a' y P[0] = 'a' coinciden (s = 0)")
    expect(frames[3].label).toBe(
      "Comparación 4: T[3] = 'b' y P[3] = 'b' coinciden (s = 0); ocurrencia en s = 0; retroceso j ← π[3] = 2",
    )
    expect(frames[6].label).toBe("Comparación 7: T[6] = 'c' y P[2] = 'a' no coinciden (s = 4); retroceso j ← π[1] = 0")
    expect(frames[7].label).toBe("Comparación 8: T[6] = 'c' y P[0] = 'a' no coinciden (s = 6); j = 0: avanza i")
  })

  it('Boyer-Moore: un frame por alineación con s y s + salto aplicado (AC-17)', () => {
    const frames = toFrames(BOYER_MOORE_TRACE_ABAB)

    expect(frames.map((frame) => frame.shift)).toEqual([0, 2, 4])
    expect(frames.map((frame) => frame.nextShift)).toEqual([2, 4, 5])
    expect(frames.map((frame) => frame.occurrence)).toEqual([true, true, false])
    expect(frames.map((frame) => frame.mismatchIndex)).toEqual([undefined, undefined, 3])
    // Comparaciones de derecha a izquierda, en el orden de la traza.
    expect(frames[0].compared.map((compared) => compared.patternIndex)).toEqual([3, 2, 1, 0])
    expect(frames[2].compared).toEqual([{ textIndex: 7, patternIndex: 3, matched: false }])
    expect(frames[0].label).toBe('Alineación 1 (s = 0): ocurrencia completa; salto tras ocurrencia 2')
    expect(frames[2].label).toBe(
      "Alineación 3 (s = 4): desajuste en j = 3 con T[7] = 'a'; carácter malo 1, sufijo bueno 1, salto aplicado 1",
    )
  })

  it('Rabin-Karp: un frame por ventana, sin comparaciones y con hash, verificación y colisión (AC-18)', () => {
    const frames = toFrames(RABIN_KARP_TRACE_ABAB)

    expect(frames).toHaveLength(5)
    expect(frames.map((frame) => frame.shift)).toEqual([0, 1, 2, 3, 4])
    expect(frames.map((frame) => frame.nextShift)).toEqual([1, 2, 3, 4, 5])
    expect(frames.map((frame) => frame.compared)).toEqual([[], [], [], [], []])
    expect(frames.map((frame) => frame.occurrence)).toEqual([true, false, true, false, false])
    expect(frames.map((frame) => frame.window?.hash)).toEqual([48, 92, 48, 94, 21])
    expect(frames[1].label).toBe('Ventana s = 1: t(1) = 92 ≠ h(P) = 48')
    expect(frames[0].label).toBe('Ventana s = 0: t(0) = 48 = h(P); verificación: coincide, ocurrencia')
  })

  it('Rabin-Karp: la ventana con hash igual y texto distinto queda como colisión (AC-18)', () => {
    const frames = toFrames(RABIN_KARP_COLLISION_TRACE)

    expect(frames.map((frame) => frame.window?.collision)).toEqual([false, false, true])
    expect(frames[2].occurrence).toBe(false)
    expect(frames[2].label).toBe('Ventana s = 2: t(2) = 0 = h(P); verificación: no coincide, colisión')
  })

  it('una traza PENDIENTE no tiene frames', () => {
    expect(toFrames(PENDING_BOYER_MOORE_TRACE)).toEqual([])
  })
})
