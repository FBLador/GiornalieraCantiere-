package com.idroambiente.giornalieradicantiere.util

/**
 * Formatta il nome della commessa nel formato [4 cifre]-[3 cifre] (es. 0001-234).
 * Il numero viene prima convertito in stringa con padding a 7 cifre.
 */
fun formatCommessaNome(nome: Int): String {
    val padded = nome.toString().padStart(7, '0')
    return "${padded.take(4)}-${padded.drop(4).take(3)}"
}

/**
 * Overload per String: usato da RiepilogoCommessaPdfData dove commessaNome è già String.
 */
fun formatCommessaNome(nome: String): String {
    return if (nome.length >= 7) {
        "${nome.take(4)}-${nome.drop(4).take(3)}"
    } else {
        nome
    }
}
