# Spec Requirements Document

> Spec: Model Inference llama.cpp Implementation
> Created: 2025-08-29
> Status: Planning

## Overview

Complete the llama.cpp inference implementation to enable on-device text simplification with real-time token streaming. This feature replaces the current stub implementation with actual model inference, enabling Crispify to simplify text to a 7th-9th grade reading level using the integrated GGUF model.

## User Stories

### Primary User Story

As a user selecting complex text in any Android app, I want to tap "Crispify" in the text toolbar, so that I can instantly see a simplified version of the text that's easier to understand.

When I select dense academic, technical, or professional text and choose Crispify from the ACTION_PROCESS_TEXT menu, the app opens a bottom sheet showing the original text, then streams the simplified version token-by-token as it's generated. The simplification preserves all key facts, names, and numbers while using shorter sentences and simpler vocabulary appropriate for a 7th-9th grade reading level.

## Spec Scope

1. **Prompt Template Integration** - Implement the PRD-specified prompt template with exact formatting for consistent text simplification
2. **Token Processing** - Pre-flight validation ensuring user input stays under 1000 tokens (reserving 200 for template)
3. **Inference Engine** - Execute llama.cpp model inference with mobile-optimized batch size (128) and proper sampling chain
4. **Token Streaming** - Stream generated tokens through existing JNI callbacks for real-time display
5. **Completion Detection** - Recognize "### End" marker (literal string) and EOS tokens for proper termination
6. **Error Handling** - Implement comprehensive error enum for clean JNI error propagation
7. **Memory Management** - Track actual model memory usage using native llama.cpp functions

## Out of Scope

- Model selection or swapping functionality
- Custom prompt templates or user-adjustable parameters
- Batch processing or multi-text handling
- Network-based model updates or cloud fallback
- Advanced chunking for texts exceeding token limits

## Expected Deliverable

1. **Functional text simplification** that processes input text through the GGUF model with proper prompt template injection
2. **Real-time token streaming** that displays generated text progressively with mobile-optimized performance (128 batch size)
3. **Robust error handling** with defined error codes (TOKEN_LIMIT_EXCEEDED, OUT_OF_MEMORY, etc.) for clean JNI propagation
4. **Token validation** that ensures user input stays under 1000 tokens with full prompt under 1200 total
5. **Proper sampling chain** implementing repetition penalty → top-k → top-p → temperature for quality output
6. **Accurate memory tracking** using native llama.cpp functions for diagnostics reporting

## Spec Documentation

- Tasks: @.agent-os/specs/2025-08-29-model-inference-llama-cpp/tasks.md
- Technical Specification: @.agent-os/specs/2025-08-29-model-inference-llama-cpp/sub-specs/technical-spec.md