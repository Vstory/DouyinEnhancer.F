package io.github.twyora.douyinenhancer.config.kvstorage

private fun Any.toKvValue(): KVStorageProto.KvValue {
    val builder = KVStorageProto.KvValue.newBuilder()
    when (this) {
        is Int -> builder.intV = this

        is Long -> builder.longV = this

        is Boolean -> builder.boolV = this

        is Float -> builder.floatV = this

        is Double -> builder.doubleV = this

        is String -> builder.stringV = this

        is Set<*> -> {
            require(
                this.all { element ->
                    element is String
                }
            ) {
                "Set elements must all be String"
            }
            val setBuilder = KVStorageProto.StringSet.newBuilder()
            this.forEach { element ->
                setBuilder.addItems(element as String)
            }
            builder.setV = setBuilder.build()
        }

        else -> throw IllegalArgumentException("unsupported type: ${this::class.qualifiedName}")
    }
    return builder.build()
}

private fun KVStorageProto.KvValue.toAnyOrNull(): Any? = when (kindCase) {
    KVStorageProto.KvValue.KindCase.BOOL_V -> boolV
    KVStorageProto.KvValue.KindCase.INT_V -> intV
    KVStorageProto.KvValue.KindCase.LONG_V -> longV
    KVStorageProto.KvValue.KindCase.FLOAT_V -> floatV
    KVStorageProto.KvValue.KindCase.DOUBLE_V -> doubleV
    KVStorageProto.KvValue.KindCase.STRING_V -> stringV
    KVStorageProto.KvValue.KindCase.SET_V -> setV.itemsList.toSet()
    KVStorageProto.KvValue.KindCase.KIND_NOT_SET -> null
}

fun IKVStorage.encodeToKVBytes(): ByteArray {
    val mapBuilder = KVStorageProto.KvMap.newBuilder()
    this.getAll().forEach { (key, value) ->
        mapBuilder.putEntries(key, value.toKvValue())
    }
    return mapBuilder.build().toByteArray()
}

fun ByteArray.decodeAsKVMap(): Map<String, Any> = buildMap {
    KVStorageProto.KvMap.parseFrom(this@decodeAsKVMap).entriesMap.forEach { (key, value) ->
        value.toAnyOrNull()?.let {
            put(key, it)
        }
    }
}
