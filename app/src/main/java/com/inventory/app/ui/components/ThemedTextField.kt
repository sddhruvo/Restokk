package com.inventory.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [OutlinedTextField] with a hand-drawn wobbly ink underline
 * in Paper & Ink mode. The native Material3 border is hidden; a custom wobble
 * underline is drawn via [Modifier.drawBehind].
 *
 * All fields get a wobbly bottom underline (like writing on ruled paper).
 * When [inkEndcaps] is true, small upward ink ticks are drawn at each end of the
 * underline, creating an open-bottom bracket `⌊___⌋` — used for interactive fields
 * (dropdowns, date pickers) to hint "tap here" without a full border box.
 *
 * Focus animates border alpha from [InkTokens.borderSubtle] → [InkTokens.borderBold].
 * Error state switches border to [MaterialTheme.colorScheme.error] with a shake animation.
 *
 * Modern mode delegates entirely to [OutlinedTextField].
 */
@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    inkEndcaps: Boolean = false,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = textStyle,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
        return
    }

    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme

    // Focus state
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) InkTokens.borderBold else InkTokens.borderSubtle,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationMedium),
        label = "textFieldBorderAlpha"
    )

    // Error shake
    val density = LocalDensity.current
    val shakeOffsetPx = with(density) { InkTokens.shakeOffset.toPx() }
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError && !reduceMotion) {
            shakeAnim.snapTo(shakeOffsetPx)
            shakeAnim.animateTo(0f, PaperInkMotion.ShakeSpring)
        }
    }

    // Border drawing params
    val borderColor = if (isError) colorScheme.error else colorScheme.onSurface
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val strokeWidthPx = with(density) { InkTokens.strokeMedium.toPx() }
    val wobbleAmplitudePx = with(density) { InkTokens.wobbleSmall.toPx() }
    // Hide native border, make container transparent
    val inkColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        errorBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .graphicsLayer { translationX = shakeAnim.value }
            .drawBehind {
                val insetPx = 4.dp.toPx()
                val endcapPx = if (inkEndcaps) 6.dp.toPx() else 0f
                val path = buildWobbleUnderlinePath(
                    width = size.width,
                    y = size.height,
                    wobbleAmplitude = wobbleAmplitudePx,
                    wobbleSeed = wobbleSeed,
                    segments = 5,
                    inset = insetPx,
                    endcapHeight = endcapPx
                )

                // Bleed layer
                drawPath(
                    path = path,
                    color = borderColor.copy(alpha = InkTokens.fillBleed),
                    style = Stroke(
                        width = strokeWidthPx * 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Primary layer
                drawPath(
                    path = path,
                    color = borderColor.copy(alpha = borderAlpha),
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            },
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        shape = shape,
        colors = inkColors,
    )
}
