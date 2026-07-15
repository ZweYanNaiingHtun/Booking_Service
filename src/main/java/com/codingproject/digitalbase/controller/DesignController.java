//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.DesignResponseDto;
import com.codingproject.digitalbase.service.DesignService;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/designs"})
@RequiredArgsConstructor
public class DesignController {
    private final DesignService designService;

    // 🌟 [POST] Admin များ ပုံအသစ် တင်ရန် API
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Admin တစ်ဦးတည်းသာ ပုံတင်ခွင့်ပြုမည်
    public ResponseEntity<DesignResponseDto> uploadDesign(
            @RequestParam(value = "title",required = false ) String title,
            @RequestParam("designImage") MultipartFile file) throws IOException {

        // ဖိုင်အလွတ်ကြီး ဖြစ်နေပါက Error သတ်မှတ်မည်
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(designService.uploadDesign(title, file));
    }

    // 🗑️ [DELETE] Admin များ Design အား ပြန်လည်ဖျက်သိမ်းရန် API (🌟 Endpoint အသစ်)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Admin တစ်ဦးတည်းသာ ဖျက်ခွင့်ပြုမည်
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build(); // Success ဖြစ်ပါက 204 No Content ပြန်မည်
    }

    @GetMapping({"/trending"})
    public ResponseEntity<List<DesignResponseDto>> getTrending(Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(this.designService.getTrendingDesigns(email));
    }

    @PostMapping({"/{id}/react"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> reactToDesign(@PathVariable Long id, Principal principal) {
        this.designService.toggleReactToDesign(id, principal.getName());
        return ResponseEntity.ok("Reaction toggled successfully");
    }

    @GetMapping({"/favorites"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<DesignResponseDto>> getMyFavorites(Principal principal) {
        return ResponseEntity.ok(this.designService.getCustomerFavorites(principal.getName()));
    }
}
