package com.gymtrack.common.rpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MembresiaPagoRpcJsonTest {
 @Test void requestYRespuestaUsanMismoMiembroIdYBooleanos() throws Exception {var mapper=new ObjectMapper();var q=new ConsultarMembresiaPago(7L);assertEquals(q,mapper.readValue(mapper.writeValueAsString(q),ConsultarMembresiaPago.class));var r=new RespuestaMembresiaPago(7L,true,false);var json=mapper.writeValueAsString(r);assertTrue(json.contains("\"tieneMembresia\":false"));assertEquals(r,mapper.readValue(json,RespuestaMembresiaPago.class));}
}
