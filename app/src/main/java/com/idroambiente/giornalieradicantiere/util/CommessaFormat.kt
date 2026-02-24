package com.idroambiente.giornalieradicantiere.util

/**
 * Formatta il nome della commessa come stringa a 7 cifre (es. 0001234).
 */
fun formatCommessaNome(nome: Int): String {
    return nome.toString().padStart(7, '0')
}

/**
 * Overload per String: usato da RiepilogoCommessaPdfData dove commessaNome è già String.
 */
fun formatCommessaNome(nome: String): String {
    return nome.padStart(7, '0')
}
