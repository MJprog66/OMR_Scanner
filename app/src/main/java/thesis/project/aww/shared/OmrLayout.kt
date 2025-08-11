package thesis.project.aww.shared

object OmrLayout {

    const val TEMPLATE_WIDTH = 595f
    const val TEMPLATE_HEIGHT = 842f

    // Start positions (match main sheet generator)
    const val startX = 56f
    const val startY = 140f

    // Layout grid for questions
    const val colWidth = 129.8f
    const val rowHeight = 23.8f
    const val questionsPerColumn = 25

    // Bubble visuals
    const val bubbleSize = 8.9f
    const val bubbleSpacing = 8.2f

    const val bubbleRadius = bubbleSize / 2f

    // Offsets for drawing relative to row origin
    const val questionNumberOffsetX = 0f
    const val questionNumberOffsetY = 0f  // Slight vertical tweak
    const val bubbleOffsetX = 21f         // Distance from number to first bubble
    const val bubbleOffsetY = 2.8f          // Keep aligned on row center

    // Multiple choice options
    val choices = listOf('A', 'B', 'C', 'D')
}