package com.vaultspec.authenticator.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

// ── Buttons ──

@Composable
private fun BaseButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    icon: ImageVector?,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = VaultSpecDimens.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (loading) containerColor else containerColor.copy(alpha = containerColor.alpha * 0.45f),
            disabledContentColor = if (loading) contentColor else contentColor.copy(alpha = 0.6f),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(VaultSpecDimens.ControlHeight),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Filled accent button — one per screen, for the main action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) = BaseButton(
    text = text,
    onClick = onClick,
    containerColor = if (destructive) LocalVaultSpecColors.current.red else MaterialTheme.colorScheme.primary,
    contentColor = Color.White,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    icon = icon,
)

/** Accent-tinted button for secondary actions. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = BaseButton(
    text = text,
    onClick = onClick,
    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    contentColor = MaterialTheme.colorScheme.primary,
    modifier = modifier,
    enabled = enabled,
    loading = false,
    icon = icon,
)

/** Neutral gray button — cancel / dismiss. */
@Composable
fun GrayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSurface: Boolean = true,
) = BaseButton(
    text = text,
    onClick = onClick,
    containerColor = if (onSurface) LocalVaultSpecColors.current.fillOnSurface else LocalVaultSpecColors.current.fill,
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
    enabled = true,
    loading = false,
    icon = null,
)

/** Text-only button for tertiary actions. */
@Composable
fun PlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
        shape = VaultSpecDimens.ControlShape,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    size: Dp = 40.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

// ── Inputs ──

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    onSurface: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val extra = LocalVaultSpecColors.current
    val container = if (onSurface) extra.fillOnSurface else MaterialTheme.colorScheme.surface
    var visible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "field_border",
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation()
            else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else null,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        shape = VaultSpecDimens.ControlShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = extra.tertiaryLabel,
            unfocusedPlaceholderColor = extra.tertiaryLabel,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, VaultSpecDimens.ControlShape),
    )
}

@Composable
fun VaultSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val extra = LocalVaultSpecColors.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) extra.green else extra.switchOff,
        label = "switch_track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "switch_thumb",
    )
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 9.dp)
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 2.dp)
                .size(26.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    onSurface: Boolean = true,
) {
    val extra = LocalVaultSpecColors.current
    val segmentShape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (onSurface) extra.fillOnSurface else extra.fill)
            .padding(2.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.shadow(1.dp, segmentShape) else Modifier)
                    .clip(segmentShape)
                    .background(if (isSelected) extra.segmentThumb else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) },
                    ),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "chip_container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "chip_content",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
    }
}

// ── Structure ──

/** Rounded colored tile with a white glyph, used at the start of list rows. */
@Composable
fun IconTile(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.27f))
            .background(color),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

/** Soft tinted circle with a glyph, used as the hero of empty / permission / dialog states. */
@Composable
fun HeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 72.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** Top bar for pushed screens: a back button and optional trailing actions. */
@Composable
fun NavBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 8.dp),
    ) {
        CircleIconButton(
            icon = Icons.Default.ArrowBackIosNew,
            contentDescription = "Back",
            onClick = onBack,
        )
        Spacer(modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun LargeTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

/** Inset grouped container for list rows. */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VaultSpecDimens.GroupShape)
            .background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}

/** Hairline between rows of a [GroupCard], inset to align with the row text. */
@Composable
fun GroupDivider(startIndent: Dp = 58.dp) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = LocalVaultSpecColors.current.separator,
        modifier = Modifier.padding(start = startIndent),
    )
}

// ── Dialog ──

@Composable
fun VaultDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    dismissText: String = "Cancel",
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val extra = LocalVaultSpecColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = VaultSpecDimens.DialogShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                if (icon != null) {
                    HeroIcon(
                        icon = icon,
                        color = if (destructive) extra.red else MaterialTheme.colorScheme.primary,
                        size = 56.dp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GrayButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        loading = loading,
                        destructive = destructive,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Inline error line shown under form fields. */
@Composable
fun ErrorText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}
