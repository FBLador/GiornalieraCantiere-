package com.idroambiente.giornalieradicantiere.util

/**
 * Formatta il nome della commessa nel formato [4 cifre]-[3 cifre] (es. 0000-123).
 * Se il nome ha meno di 7 caratteri, viene restituito invariato.
 */
fun formatCommessaNome(nome: String): String {
    return if (nome.length >= 7) {
        "${nome.take(4)}-${nome.drop(4).take(3)}"
    } else {
        nome
    }
}
