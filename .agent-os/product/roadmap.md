# Product Roadmap

## Phase 1: MVP - Core Simplification

**Goal:** Validate the concept that on-device text simplification provides genuine value
**Success Criteria:** Successfully simplify text from any app with <2 second processing time for typical paragraphs

### Features

- [X] First Launch screen - Onboarding and initial model load `S`
- [X] Process-Text intent handler - Receive text from Android selection toolbar `M`
- [X] Bottom sheet UI - Display simplified text in overlay `S` [COMPLETED 2025-08-29]
- [X] Test infrastructure fixes - Resolve coroutine timing and mock configuration issues `M`
- [ ] LLM integration - Implement llama.cpp JNI wrapper `L`
- [ ] Text leveling engine - Implement 7th-grade simplification prompt `M`
- [ ] Text streaming - Display partial results as generated `M`
- [ ] Copy functionality - Allow copying simplified text `XS`
- [ ] Model delivery - Configure Play Asset Delivery for GGUF model `M`
- [ ] Privacy validation - Verify no network permissions in manifest `XS`
- [ ] Basic error handling - Graceful failures for edge cases `S`

### Dependencies

- Selection and configuration of appropriate GGUF model
- Play Console setup for asset delivery
- Testing devices with varying RAM configurations

## Phase 2: Polish & Performance

**Goal:** Create a delightful user experience with consistent performance
**Success Criteria:** 95% of simplifications complete in under 1.5 seconds with smooth animations

### Features

- [ ] Loading animations - Smooth transitions during processing `S`
- [ ] Model optimization - Fine-tune quantization for speed `L`
- [ ] Memory management - Optimize for low-RAM devices `M`
- [ ] Haptic feedback - Subtle feedback on interactions `XS`

### Dependencies

- User feedback from Phase 1
- Performance profiling data
- Broader device testing pool

## Phase 3: Enhanced Intelligence

**Goal:** Expand beyond basic simplification to provide more intelligent text processing
**Success Criteria:** Users report 2x improvement in comprehension speed for technical content

### Features

- [ ] Context awareness - Maintain domain-specific accuracy `L`
- [ ] Summary mode - Generate concise summaries `M`
- [ ] Definition tooltips - Explain key terms inline `M`
- [ ] Multi-level simplification - Adjustable reading levels `S`
- [ ] Batch processing - Simplify multiple selections `S`

### Dependencies

- Potential model upgrades or fine-tuning
- Expanded prompt engineering
- User research on comprehension needs