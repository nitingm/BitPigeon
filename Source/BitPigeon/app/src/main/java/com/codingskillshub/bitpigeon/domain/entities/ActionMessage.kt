package com.codingskillshub.bitpigeon.domain.entities

import java.io.Serializable

data class ActionMessage(
    val actionType: String,
    val data: Any
) : Serializable {

}
