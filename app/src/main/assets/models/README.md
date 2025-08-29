# Model Assets Directory

This directory contains the GGUF model files for Crispify.

## Development Model

For development, place the following model here:
- `gemma-3-270m-it-Q4_K_M.gguf` - Gemma 3 270M Instruct model, Q4_K_M quantization

## Model Requirements

- **Format**: GGUF (GPT-Generated Unified Format)
- **Quantization**: Q4_K_M (4-bit quantization, K-means, Medium)
- **Size**: ~150-200MB for 270M parameter model
- **Architecture**: arm64-v8a compatible

## Production Notes

In production, models will be delivered via Google Play Asset Delivery as install-time assets.
This directory is only used for development builds.

## Obtaining the Model

1. Download from Hugging Face or similar repository
2. Ensure it's the Q4_K_M quantized version
3. Place in this directory with exact filename: `gemma-3-270m-it-Q4_K_M.gguf`

## Placeholder

For CI/CD builds without the actual model, a small placeholder file can be used.
The app will detect and handle missing/invalid models gracefully.