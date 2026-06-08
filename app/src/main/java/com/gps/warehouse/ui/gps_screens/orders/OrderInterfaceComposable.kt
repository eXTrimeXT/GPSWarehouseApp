package com.gps.warehouse.ui.gps_screens.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gps.warehouse.data.remote.gps_dto.OrderDto


// Блок статуса для заказа (иконка+цвет)
@Composable
fun RowHeaderAndStatusOrder(order: OrderDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = order.orderNumber,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        // Блок статуса с иконкой и цветом
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            val statusText = when (order.status) {
                "done" -> "Подтвержден"
                "dispute" -> "Не подтвержден"
                "new" -> "Новый"
                "sent" -> "Сформирован и отправлен"
                else -> "В пути"
            }

            val statusColor = when (order.status) {
                "done" -> Color(0, 150, 0, 255)
                "dispute" -> Color.Red
                "new" -> Color.Black
                "sent" -> Color.Black
                "inway" -> Color.Black
                else -> Color(0, 0, 0, 0)
            }

            val statusIcon = when (order.status) {
                "done" -> Icons.Default.CheckCircle
                "dispute" -> Icons.Default.Error
                "new" -> Icons.Default.AddCircle
                "sent" -> Icons.Default.PresentToAll
                "inway" -> Icons.Default.AirportShuttle
                else -> Icons.Default.QuestionMark
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

// SAP и Количество
@Composable
fun RowSapAndQTY(order: OrderDto){
    // 2. SAP и Количество
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = order.sapOrder ?: "SAP отсутствует",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val qty = if (order.qty.isNullOrEmpty()) "0" else order.qty
        Text(
            text = "Позиций: ${qty}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Блок для дат
@Composable
fun DateRow(label: String, value: String, isHighlight: Boolean = false) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Единая карточка заказа как для "Списка заказов" так и для "Архива"
@Composable
fun OrderCard(order: OrderDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Блок статуса
            RowHeaderAndStatusOrder(order)
            // SAP и Количество
            RowSapAndQTY(order)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Даты
            DateRow(label = "Создан:", value = order.dateCreate)
            if (!order.dateSent.isNullOrEmpty()) {
                DateRow(label = "Отправлен:", value = order.dateSent)
            }
            if (!order.dateInWay.isNullOrEmpty()) {
                DateRow(label = "В пути:", value = order.dateInWay)
            }
            if (!order.dateDone.isNullOrEmpty()) {
                DateRow(label = "Выполнен:", value = order.dateDone, isHighlight = true)
            }
        }
    }
}

