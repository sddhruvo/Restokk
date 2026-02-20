package com.inventory.app.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.repository.ShoppingListRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddToShoppingListReceiver : BroadcastReceiver() {

    @Inject
    lateinit var shoppingListRepository: ShoppingListRepository

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (itemId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Only add if not already on shopping list
                val existing = shoppingListRepository.findActiveByItemId(itemId)
                if (existing == null) {
                    shoppingListRepository.addItem(
                        ShoppingListItemEntity(itemId = itemId, quantity = 1.0)
                    )
                }
                // Dismiss the notification
                if (notificationId != -1) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
