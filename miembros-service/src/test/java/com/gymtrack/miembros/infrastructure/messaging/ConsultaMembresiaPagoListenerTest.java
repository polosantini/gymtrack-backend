package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.common.rpc.*;
import com.gymtrack.miembros.domain.model.*;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsultaMembresiaPagoListenerTest {
 @Test void informaSinMembresiaYNoCreaNada(){var repo=mock(MiembroRepository.class);when(repo.findById(7L)).thenReturn(Optional.of(new Miembro()));var r=new ConsultaMembresiaPagoListener(repo).consultar(new ConsultarMembresiaPago(7L));assertEquals(new RespuestaMembresiaPago(7L,true,false),r);verify(repo,never()).save(any());}
 @Test void informaMiembroInexistente(){var repo=mock(MiembroRepository.class);when(repo.findById(9L)).thenReturn(Optional.empty());assertEquals(new RespuestaMembresiaPago(9L,false,false),new ConsultaMembresiaPagoListener(repo).consultar(new ConsultarMembresiaPago(9L)));}
 @Test void dejaPagarMembresiaSuspendidaVencidaOActiva(){var repo=mock(MiembroRepository.class);var m=new Miembro();var mem=new Membresia();m.setMembresia(mem);when(repo.findById(1L)).thenReturn(Optional.of(m));var listener=new ConsultaMembresiaPagoListener(repo);for(var estado:EstadoMembresia.values()){mem.setEstado(estado);assertEquals(new RespuestaMembresiaPago(1L,true,true),listener.consultar(new ConsultarMembresiaPago(1L)));}}
 @Test void consultaInvalidaSeRechaza(){var repo=mock(MiembroRepository.class);assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class,()->new ConsultaMembresiaPagoListener(repo).consultar(new ConsultarMembresiaPago(null)));verifyNoInteractions(repo);}
}
