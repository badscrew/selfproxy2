package com.selfproxy.vpn.data.database

import androidx.room.TypeConverter
import com.selfproxy.vpn.domain.model.Protocol

/**
 * Type converters for Room database.
 * 
 * Converts custom types to and from database-compatible types.
 */
class Converters {
    
    /**
     * Converts Protocol enum to String for database storage.
     */
    @TypeConverter
    fun fromProtocol(protocol: Protocol): String {
        return protocol.name
    }
    
    /**
     * Converts String from database to Protocol enum.
     */
    @TypeConverter
    fun toProtocol(value: String): Protocol {
        return Protocol.valueOf(value)
    }
}
