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

    // Храним полный текущий список пользователей для отправки на сервер
    val currentUsers: List<AssetUserFullResponse> = emptyList(),
    val currentResponsibleUsers: List<AssetUserFullResponse> = emptyList(),
    val currentServingUsers: List<AssetUserFullResponse> = emptyList()
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

                currentUsers = asset.users ?: emptyList(),
                currentResponsibleUsers = asset.responsibleUsers ?: emptyList(),
                currentServingUsers = asset.servingUsers ?: emptyList()
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

            // Отправляем полные списки пользователей, если они изменились
            users = currentUsers,
            responsibleUsers = currentResponsibleUsers,
            servingUsers = currentServingUsers
        )
    }

    // Методы для управления пользователями
    fun addUser(type: UserType, employee: EmployeeShortResponse): AssetEditState {
        val newUser = AssetUserFullResponse(
            guid = employee.guid,
            employeeId = employee.employeeId,
            fullNameRu = employee.fullNameRu ?: employee.employeeId,
            fullNameEn = employee.fullNameEn  ?: employee.employeeId,
            position = employee.position,
            department = employee.department,
            division = employee.division,
            group = employee.group,
            society = employee.society,
            phone = employee.phone,
            email = employee.email,
            comment = employee.comment,
            birthDate = null,
            employmentDate = null,
            dismissalDate = null,
            positionGuid = null,
            departmentGuid = null,
            createdAt = null,
            updatedAt = null,
            startDate = null,
            endDate = null,
            assignmentType = when (type) {
                UserType.USER -> "user"
                UserType.RESPONSIBLE -> "responsible"
                UserType.SERVING -> "serving"
            }
        )
        return when (type) {
            UserType.USER -> copy(currentUsers = currentUsers + newUser)
            UserType.RESPONSIBLE -> copy(currentResponsibleUsers = currentResponsibleUsers + newUser)
            UserType.SERVING -> copy(currentServingUsers = currentServingUsers + newUser)
        }
    }

    fun removeUser(type: UserType, userGuid: String): AssetEditState {
        return when (type) {
            UserType.USER -> copy(currentUsers = currentUsers.filter { it.guid != userGuid })
            UserType.RESPONSIBLE -> copy(currentResponsibleUsers = currentResponsibleUsers.filter { it.guid != userGuid })
            UserType.SERVING -> copy(currentServingUsers = currentServingUsers.filter { it.guid != userGuid })
        }
    }
}