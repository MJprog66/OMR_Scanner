package thesis.project.aww.shared

object OmrLayout {

    const val TEMPLATE_WIDTH = 595f
    const val TEMPLATE_HEIGHT = 842f

    // Start positions (match main sheet generator)
    const val startX = 56f
    const val startY = 141f

    // Layout grid for questions
    const val colWidth = 129.6f
    const val rowHeight = 24f
    const val questionsPerColumn = 25

    // Bubble visuals
    const val bubbleSize = 8f
    const val bubbleSpacing = 9f
    const val bubbleRadius = bubbleSize / 2f

    // Offsets for drawing relative to row origin
    const val questionNumberOffsetX = 0f
    const val questionNumberOffsetY = 4f  // Slight vertical tweak
    const val bubbleOffsetX = 20f         // Distance from number to first bubble
    const val bubbleOffsetY = 0f          // Keep aligned on row center

    // Multiple choice options
    val choices = listOf('A', 'B', 'C', 'D')
}
