package com.ing.zkflow.util

/**
 * Merges two maps by associating keys from both maps to a list combined from both respective values.
 */
fun <K, V> Map<K, List<V>>.merge(that: Map<K, List<V>>): Map<K, List<V>> =
    (this.keys + that.keys).associateWith { key ->
        (this[key] ?: emptyList()) + (that[key] ?: emptyList())
    }
