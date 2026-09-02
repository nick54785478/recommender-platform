package com.omni.recommender.behavior.presentation.controller;

import com.omni.recommender.behavior.application.port.in.LogBehaviorUseCase;
import com.omni.recommender.behavior.presentation.assembler.BehaviorResourceAssembler;
import com.omni.recommender.behavior.presentation.resource.in.LogBehaviorResource;
import com.omni.recommender.behavior.presentation.resource.out.BehaviorLoggedResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收用戶行為打點的 REST Controller
 */
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final LogBehaviorUseCase logBehaviorUseCase;
    private final BehaviorResourceAssembler assembler;

    @PostMapping("/log")
    public ResponseEntity<BehaviorLoggedResource> logBehavior(@Valid @RequestBody LogBehaviorResource resource) {
        
        // 1. Assembler: Resource -> Command
        var command = assembler.toCommand(resource);
        
        // 2. 呼叫 Application Layer
        logBehaviorUseCase.execute(command);
        
        // 3. 回傳成功結果 (因寫入是非同步，此處立即返回 202 Accepted)
        var response = BehaviorLoggedResource.builder()
                .status("ACCEPTED")
                .message("Behavior log received successfully")
                .build();
                
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
