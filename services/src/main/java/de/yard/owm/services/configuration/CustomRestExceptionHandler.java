package de.yard.owm.services.configuration;

import de.yard.owm.dto.BaseResponse;
import de.yard.owm.misc.GeneralOwmException;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class CustomRestExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Handling AccessDeniedException");
        return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MeshInconsistencyException.class)
    @ResponseBody
    public ResponseEntity<Object> handleMeshInconsistencyException(MeshInconsistencyException ex) {
        log.debug("Handling MeshInconsistencyException");
        // TODO build error response
        return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(GeneralOwmException.class)
    @ResponseBody
    public ResponseEntity<BaseResponse> handleGeneralOwmException(GeneralOwmException ex) {
        log.debug("Handling GeneralOwmException");
        BaseResponse response = new BaseResponse();
        response.setError(ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
