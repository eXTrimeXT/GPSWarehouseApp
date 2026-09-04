package com.gps.warehouse.data.remote.assets_dto

// Enum для типа пользователя
enum class UserType {
    USER, RESPONSIBLE, SERVING
}

data class AssetEditState(
    val name: String? = null,
    val inventoryId: String? = null,
    val serialNumber: String? = null,
    val assetStatusId: Int? = null,
    val quantity: Int? = null,
    val comment: String? = null,
    val dateIssue: String? = null,
    val datePurchasing: String? = null,
    val modelId: Int? = null,
    val modelName: String? = null,
    val assetTypeId: Int? = null,
    val parentId: Int? = null,
    val everyWeekCheck: Boolean? = null,
    val nextService: String? = null,
    val servicePeriod: Int? = null,
    val location: AssetLocationUpdate? = null,
    val currentUser: String? = null,

    val usersToAdd: List<AssetUserUpdate> = emptyList(),
    val usersToRemove: List<String> = emptyList(),
    val responsibleUsersToAdd: List<AssetUserUpdate> = emptyList(),
    val responsibleUsersToRemove: List<String> = emptyList(),
    val servingUsersToAdd: List<AssetUserUpdate> = emptyList(),
    val servingUsersToRemove: List<String> = emptyList()
) {
    companion object {
        /** Создаёт состояние из существующего актива */
        fun fromAsset(asset: AssetResponseDto): AssetEditState {
            return AssetEditState(
                name = asset.name,
                inventoryId = asset.inventoryId,
                serialNumber = asset.serialNumber,
                assetStatusId = asset.assetStatusId,
                quantity = asset.quantity,
                comment = asset.comment,
                dateIssue = asset.dateIssue,
                datePurchasing = asset.datePurchasing,
                modelId = asset.modelId,
                modelName = asset.modelName,
                assetTypeId = asset.assetTypeId,
                parentId = asset.parentId,
                everyWeekCheck = asset.everyWeekCheck,
                nextService = asset.nextService,
                servicePeriod = asset.servicePeriod,
                currentUser = asset.currentUser,
                location = asset.location?.let {
                    AssetLocationUpdate(it.workshopId, it.place, it.level, it.x ?: 0, it.y ?: 0)
                },

                usersToAdd = emptyList(),
                usersToRemove = emptyList(),
                responsibleUsersToAdd = emptyList(),
                responsibleUsersToRemove = emptyList(),
                servingUsersToAdd = emptyList(),
                servingUsersToRemove = emptyList()
            )
        }
    }

    /** Преобразует в AssetUpdate, отправляя только изменённые поля */
    fun toUpdate(original: AssetResponseDto): AssetUpdate {
        return AssetUpdate(
            name = name.takeIf { it != original.name },
            inventoryId = inventoryId.takeIf { it != original.inventoryId },
            serialNumber = serialNumber.takeIf { it != original.serialNumber },
            assetStatusId = assetStatusId.takeIf { it != original.assetStatusId },
            quantity = quantity.takeIf { it != original.quantity },
            comment = comment.takeIf { it != original.comment },
            dateIssue = dateIssue.takeIf { it != original.dateIssue },
            datePurchasing = datePurchasing.takeIf { it != original.datePurchasing },
            modelId = modelId.takeIf { it != original.modelId },
            modelName = modelName.takeIf { it != original.modelName },
            assetTypeId = assetTypeId.takeIf { it != original.assetTypeId },
            parentId = parentId.takeIf { it != original.parentId },
            everyWeekCheck = everyWeekCheck.takeIf { it != original.everyWeekCheck },
            nextService = nextService.takeIf { it != original.nextService },
            servicePeriod = servicePeriod.takeIf { it != original.servicePeriod },
            currentUser = currentUser.takeIf { it != original.currentUser },
            location = location.takeIf { it != original.location },

            // Списки пользователей для обновления
            users = buildUserUpdateList(
                original = original.users,
                toAdd = usersToAdd,
                toRemove = usersToRemove
            ),
            responsibleUsers = buildUserUpdateList(
                original = original.responsibleUsers,
                toAdd = responsibleUsersToAdd,
                toRemove = responsibleUsersToRemove
            ),
            servingUsers = buildUserUpdateList(
                original = original.servingUsers,
                toAdd = servingUsersToAdd,
                toRemove = servingUsersToRemove
            )
        )
    }

    // Вспомогательный метод для формирования списка пользователей
    private fun buildUserUpdateList(
        original: List<AssetUserFullResponse>?,
        toAdd: List<AssetUserUpdate>,
        toRemove: List<String>
    ): List<AssetUserUpdate>? {
        if (toAdd.isEmpty() && toRemove.isEmpty()) return null

        val existingGuids = original?.map { it.guid }.orEmpty()
        val remainingGuids = existingGuids - toRemove.toSet()

        // Формируем список: существующие (оставшиеся) + новые
        return (remainingGuids.map { AssetUserUpdate(employeeId = "") } + toAdd)
            .takeIf { it.isNotEmpty() }
    }

    // Методы для управления пользователями
    fun addUser(type: UserType, employee: EmployeeShortResponse): AssetEditState {
        val newUpdate = AssetUserUpdate(employeeId = employee.employeeId)
        return when (type) {
            UserType.USER -> copy(usersToAdd = usersToAdd + newUpdate)
            UserType.RESPONSIBLE -> copy(responsibleUsersToAdd = responsibleUsersToAdd + newUpdate)
            UserType.SERVING -> copy(servingUsersToAdd = servingUsersToAdd + newUpdate)
        }
    }

    fun removeUser(type: UserType, userGuid: String): AssetEditState {
        return when (type) {
            UserType.USER -> copy(usersToRemove = usersToRemove + userGuid)
            UserType.RESPONSIBLE -> copy(responsibleUsersToRemove = responsibleUsersToRemove + userGuid)
            UserType.SERVING -> copy(servingUsersToRemove = servingUsersToRemove + userGuid)
        }
    }
}