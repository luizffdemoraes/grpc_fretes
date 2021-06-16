package br.com.zup.edu

import com.google.protobuf.Any
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FretesGrpcServer : FretesServiceGrpc.FretesServiceImplBase() {

    private val logger = LoggerFactory.getLogger(FretesGrpcServer::class.java)

    override fun calculaFrete(request: CalculaFreteRequest?, responseObserver: StreamObserver<CalculaFreteResponse>?) {

        logger.info("Calculando frete para request: $request")

        val cep = request?.cep
        //verificar cep null
        if(cep == null || cep.isBlank()) {
            val error = Status.INVALID_ARGUMENT
                .withDescription("cep deve ser informado")
                .asRuntimeException()
            responseObserver?.onError(error)
        }

        // SIMULAR uma verificação de segurança
        if(cep!!.endsWith("333")){

            val statusProto = com.google.rpc.Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED.number)
                .setMessage("usuario não pode acessar esse recurso")
                .addDetails(Any.pack(ErrorDetails.newBuilder()
                    .setCode(401)
                    .setMessage("token expirado")
                    .build()))
                .build()

            val error = StatusProto.toStatusRuntimeException(statusProto)
            responseObserver?.onError(error)
        }

        //verificar cep valido
        if (!cep!!.matches(regex = "[0-9]{5}-[0-9]{3}".toRegex())){
            val error = Status.INVALID_ARGUMENT
                .withDescription("cep inválido")
                .augmentDescription("formato esperado deve ser 99999-999")
                .asRuntimeException()
            responseObserver?.onError(error)
        }

        /*
        Exceções padrão do GRPC
        -> StatusRuntimeException - representa um erro de status do GRPC do tipo Runtime
        -> StatusException - usado para mensagens checadas
        IllegalArgumentException

        exemplo surge a mensagem para o cliente porem quase correto pois não e necessario instanciar:

         val error = StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("cep deve ser informado"))
         responseObserver?.onError(error)
         */

        //logica para tratar erros inesperados
        var valor = 0.0
        try {
            valor = Random.nextDouble(from = 0.0, until = 140.0) //logica complexa
            if(valor > 100.0){
               throw IllegalArgumentException("Erro inesperado ao executar logica de negócio!")
            }

        } catch (error: Exception){
            responseObserver?.onError(Status.INTERNAL
                .withDescription(error.message)
                .withCause(error) //anexado ao Status, mas não enviado ao Cliente
                .asRuntimeException()
            )

        }


        val response = CalculaFreteResponse.newBuilder()
            .setCep(request!!.cep)
            .setValor(valor)
            .build()

        logger.info("Frete Calculado $response")

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }

}