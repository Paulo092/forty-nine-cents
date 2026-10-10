package com.fortyninecents.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.fortyninecents.util.Money

/**
 * Mostra os dígitos digitados como moeda, estilo "caixa eletrônico":
 * "4" -> R$ 0,04, "40" -> R$ 0,40, "4000" -> R$ 40,00.
 * O cursor fica sempre no fim, então o mapeamento de offsets é trivial.
 */
class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = Money.format(Money.digitsToCents(text.text))
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = formatted.length
                override fun transformedToOriginal(offset: Int): Int = text.length
            },
        )
    }

    override fun equals(other: Any?): Boolean = other is CurrencyVisualTransformation
    override fun hashCode(): Int = javaClass.hashCode()
}
