# Sekrets

Sekrets is a console-based image steganography experiment that hides text inside image pixels using parity-based LSB-style encoding and a key-driven pixel traversal.

## Current Design

The current encoder and decoder use this framing model:

1. Convert the input string to UTF-8 bytes.
2. Write a fixed 32-bit header containing payload byte length.
3. Write payload bits using keyed traversal over image pixels.
4. Decode by reading the same 32-bit length header, then exactly that many bytes.

This keeps encoder and decoder deterministic and avoids ambiguous message termination.

## Why UTF-8?

A previous version of this program used fixed 16-bit-per-character model, and even earlier a highest order bit counting+length encoding. Those worked but were inefficient for most practical text.

- English and basic ASCII text usually needs 1 byte in UTF-8, but 2 bytes in UTF-16-like char storage. The length counting version counted and encoded 6 and 7 continuously for each character. Which was wasteful at best.
- UTF-8 still supports international text and symbols without custom character handling logic.
- A byte-length header is simpler and more reliable than scanning for an EOF marker.

In short:

- Better space efficiency for typical text.
- Full Unicode compatibility through built-in Java APIs.
- Cleaner decode stop condition.

## Bit Embedding Strategy

The code uses parity-based bit embedding:

- Bit 0 forces an even channel value.
- Bit 1 forces an odd channel value.
- If a channel already matches required parity, it is left unchanged.
- If not, it is adjusted by 1 while respecting boundaries (0 and 255).

This keeps visual change minimal while making decode straightforward.

## Keyed Pixel Traversal

Payload bits are not written sequentially. They are written using a numeric key interpreted as skip/count pairs.

This adds an extra layer beyond plain LSB embedding:

- Without the correct key, even correct parity extraction will not reconstruct the right bit order.
- Key cycling is deterministic and mirrored by the decoder.

## File Flow

- Input image: Desktop image.jpg/image.jpeg/image.png/image.gif
- Encoded output: Desktop imageCOMPLETE with matching extension marker
- Encoder entry point: EncodeToImage.java
- Decoder entry point: DecodeFromImage.java


[Placeholder. 32-bit byte-length header followed by payload bytes]

[Placeholder. a pixel grid diagram showing skip/count behavior and bit placement order]

[...more?]
