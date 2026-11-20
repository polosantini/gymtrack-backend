package com.gymtrack.pagos.infrastructure.messaging;
import com.gymtrack.common.rpc.*;
import com.gymtrack.pagos.domain.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.*;
import org.springframework.core.ParameterizedTypeReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class ValidadorMembresiaAmqpTest {
 private RabbitTemplate rabbit;
 private ValidadorMembresiaAmqp validador;
 @BeforeEach void preparar(){rabbit=mock(RabbitTemplate.class);validador=new ValidadorMembresiaAmqp(rabbit,2500);}
 @SuppressWarnings("unchecked")
 private void responder(RespuestaMembresiaPago r){when(rabbit.convertSendAndReceiveAsType(eq(MembresiaPagoRpc.EXCHANGE),eq(MembresiaPagoRpc.ROUTING_KEY),any(ConsultarMembresiaPago.class),any(MessagePostProcessor.class),any(ParameterizedTypeReference.class))).thenReturn(r);}
 @Test void permiteMembresiaExistente(){responder(new RespuestaMembresiaPago(7L,true,true));assertDoesNotThrow(()->validador.validar(7L));}
 @Test void rechazaMiembroSinMembresia(){responder(new RespuestaMembresiaPago(7L,true,false));assertThrows(MiembroSinMembresiaException.class,()->validador.validar(7L));}
 @Test void rechazaMiembroInexistente(){responder(new RespuestaMembresiaPago(7L,false,false));assertThrows(MiembroPagoNoEncontradoException.class,()->validador.validar(7L));}
 @Test void timeoutNoConcedePermiso(){responder(null);assertThrows(ValidacionPagoNoDisponibleException.class,()->validador.validar(7L));}
 @SuppressWarnings("unchecked") @Test void falloAmqpEsControlado(){when(rabbit.convertSendAndReceiveAsType(anyString(),anyString(),any(Object.class),any(MessagePostProcessor.class),any(ParameterizedTypeReference.class))).thenThrow(new AmqpException("Broker caido"));assertThrows(ValidacionPagoNoDisponibleException.class,()->validador.validar(7L));}
 @Test void identidadIncorrectaODatosIncompletosNoConcedenPermiso(){responder(new RespuestaMembresiaPago(8L,true,true));assertThrows(ValidacionPagoNoDisponibleException.class,()->validador.validar(7L));responder(new RespuestaMembresiaPago(7L,true,null));assertThrows(ValidacionPagoNoDisponibleException.class,()->validador.validar(7L));}
 @Test void configuraTimeoutAcotadoYRechazaValorInseguro(){verify(rabbit).setReplyTimeout(2500);verify(rabbit).setMandatory(true);assertThrows(IllegalArgumentException.class,()->new ValidadorMembresiaAmqp(rabbit,60000));}
}
