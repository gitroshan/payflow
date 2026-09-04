package com.payflow.gateway.api;

import com.payflow.gateway.api.GatewayDtos.*;
import com.payflow.gateway.service.MockPspService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final MockPspService psp;

    public GatewayController(MockPspService psp) {
        this.psp = psp;
    }

    @PostMapping("/authorize")
    public GatewayResponse authorize(@Valid @RequestBody AuthorizeRequest req) {
        return psp.authorize(req);
    }

    @PostMapping("/capture")
    public GatewayResponse capture(@Valid @RequestBody CaptureRequest req) {
        return psp.capture(req);
    }

    @PostMapping("/refund")
    public GatewayResponse refund(@Valid @RequestBody RefundRequest req) {
        return psp.refund(req);
    }
}
