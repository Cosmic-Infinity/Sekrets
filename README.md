# Sekrets

A console-based image steganography experiment that hides text inside image pixels using parity-based LSB-style encoding and a key-driven pixel traversal.

## How It Works

1. Convert the input string to UTF-8 bytes.
2. Write a fixed 32-bit header containing the payload byte length (obfuscated with a key-derived mask).
3. Write payload bits using keyed traversal over image pixels.
4. Decode by reading the same 32-bit header, unmasking it, then reading exactly that many bytes.

This keeps encoder and decoder deterministic and avoids ambiguous message termination.

## UTF-8 Notes

A previous version of this program used fixed 16-bit-per-character model, and even earlier a custom highest order bit counting + length encoding. Those worked but were inefficient for most practical text.

- English and basic ASCII text usually needs 1 byte in UTF-8, but 2 bytes in UTF-16-like char storage. The length counting version counted and encoded 6 and 7 continuously for each character. Which was wasteful at best.
- UTF-8 still supports international text and symbols without custom character handling logic.
- A byte-length header is simpler and more reliable than scanning for an EOF marker.

In short:

- Better space efficiency for typical text.
- Full Unicode compatibility through built-in Java APIs.
- Cleaner decode stop condition.

The encoding is UTF-8, but console input/output may not render all Unicode properly in every terminal. Emojis and some scripts can appear malformed even though the stored bytes decode correctly.

## Bit Embedding Strategy

The code uses parity-based bit embedding:

- Bit 0 forces an even channel value.
- Bit 1 forces an odd channel value.
- If a channel already matches required parity, it is left unchanged.
- If not, it is adjusted by 1 while respecting boundaries (0 and 255).

This keeps visual change minimal while making decode straightforward.

## Keyed Pixel Traversal

Payload bits are not written sequentially. They are written using a numeric key interpreted as skip/run-length pairs.

- Key format: 10 digits (5 pairs).
- Pair structure: skip (0-9) and run length (1-9).
- The key cycles deterministically; decoder mirrors the same traversal.

Without the correct key, even correct parity extraction will not reconstruct the right bit order.

## Header Masking

The 32-bit payload length header is XOR-obfuscated using a deterministic mask derived from the key. This prevents trivial length discovery without the key.

## File Flow

- Input image: Desktop image.jpg / image.jpeg / image.png / image.gif
- Encoded output: Desktop imageCOMPLETE.<same extension>
- Encoder entry point: EncodeToImage.java
- Decoder entry point: DecodeFromImage.java

The decoder selects the newest file on the Desktop matching imagecomplete* with a supported extension.


[Placeholder. 32-bit byte-length header followed by payload bytes]

[Placeholder. a pixel grid diagram showing skip/count behavior and bit placement order]

[...more?]

## Usage

1. Place an image named image.jpg (or image.jpeg/png/gif) on your Desktop.
2. Run EncodeToImage and follow the prompts for message and key choice.
3. Share the output image and the key.
4. Run DecodeFromImage and provide the key to recover the message.

## Debug Mode

Debug rendering is controlled by SekretsUtil.DEBUG_MODE:

- 0 = off (default)
- 1 = white
- 2 = black
- 3 = channel visualization
