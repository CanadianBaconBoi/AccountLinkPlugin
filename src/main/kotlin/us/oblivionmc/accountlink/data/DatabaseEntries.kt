package us.oblivionmc.accountlink.data

import java.math.BigInteger

class TokenEntry {
    lateinit var token_id: ByteArray
    lateinit var minecraft_uuid: ByteArray
}
class UnlinkTokenEntry {
    lateinit var token_id: ByteArray
    lateinit var minecraft_uuid: ByteArray
    var discord_id: BigInteger = BigInteger.ZERO
}
class LinkedAccountEntry {
    lateinit var minecraft_uuid: ByteArray
    var discord_id: BigInteger = BigInteger.ZERO
}