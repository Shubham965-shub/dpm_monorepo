import { useRef, type MouseEvent } from 'react'

/**
 * A modal backdrop's click-outside-to-close handler - closes only when BOTH the mousedown and the
 * click landed on the backdrop itself (e.target === e.currentTarget), not merely when the click
 * bubbled there. A plain `onClick={onClose}` on the backdrop (even paired with the card's own
 * `onClick={(e) => e.stopPropagation()}`) still misfires when someone starts selecting text or
 * dragging inside the card and their pointer drifts onto the backdrop before releasing - the
 * mouseup (and the synthetic click that follows it) lands on the backdrop even though the gesture
 * began inside the card, closing the modal out from under a selection. Tracking where the
 * mousedown itself started fixes that, and makes the card's own stopPropagation unnecessary.
 */
export function useBackdropClose(onClose: () => void) {
  const mouseDownOnBackdrop = useRef(false)
  return {
    onMouseDown: (e: MouseEvent) => {
      mouseDownOnBackdrop.current = e.target === e.currentTarget
    },
    onClick: (e: MouseEvent) => {
      if (mouseDownOnBackdrop.current && e.target === e.currentTarget) onClose()
    },
  }
}
