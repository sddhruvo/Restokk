package com.inventory.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

import com.inventory.app.MainActivity

class MediumWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)

        provideContent {
            GlanceTheme(colors = WidgetColorProviders) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                ) {
                    // Header row: app name + stat badges
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restokk",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        // Shopping badge
                        Text(
                            text = "\uD83D\uDED2 ${data.shoppingCount}",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        // Expiring badge
                        Text(
                            text = "\u23F0 ${data.expiringCount}",
                            style = TextStyle(
                                color = WidgetExpiringColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Divider (thin colored bar)
                    Spacer(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlanceTheme.colors.outline)
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    if (data.shoppingItems.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Shopping list is empty",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Tap to add items",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else {
                        // Shopping items list
                        LazyColumn(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                        ) {
                            items(data.shoppingItems) { item ->
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "\u2022",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.primary,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onBackground,
                                            fontSize = 13.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight(),
                                        maxLines = 1
                                    )
                                    if (item.quantity != 1.0 || item.unitAbbreviation != null) {
                                        val qtyText = buildString {
                                            val qtyDisplay = if (item.quantity == item.quantity.toLong().toDouble()) {
                                                item.quantity.toLong().toString()
                                            } else {
                                                item.quantity.toString()
                                            }
                                            append(qtyDisplay)
                                            item.unitAbbreviation?.let { append(" $it") }
                                        }
                                        Text(
                                            text = qtyText,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // "+N more" footer
                        val remaining = data.shoppingCount - data.shoppingItems.size
                        if (remaining > 0) {
                            Text(
                                text = "+$remaining more",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumWidget()
}
