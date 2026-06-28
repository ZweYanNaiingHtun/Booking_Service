//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.DesignResponseDto;
import com.codingproject.digitalbase.model.Design;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.DesignRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {
    private final DesignRepository designRepository;
    private final UserRepository userRepository;
    private final String UPLOAD_DIR = "uploads/designs/";

    @Override
    @Transactional
    public DesignResponseDto uploadDesign(String title, MultipartFile file) throws IOException {
        // ၁။ Folder မရှိသေးပါက ဆောက်ပေးခြင်း
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // ၂။ ဖိုင်နာမည် မထပ်စေရန် UUID ဖြင့် ပြောင်းလဲခြင်း
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // ၃။ ပုံကို သတ်မှတ်ထားသော ရွှေလမ်းကြောင်းထဲသို့ ကူးယူသိမ်းဆည်းခြင်း
        Path filePath = Paths.get(UPLOAD_DIR + uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // ၄။ Frontend က လှမ်းခေါ်ရမည့် နှိုင်းရ URL လမ်းကြောင်း သတ်မှတ်ခြင်း
        String imageUrl = "/uploads/designs/" + uniqueFileName;

        // ၅။ DB ထဲတွင် Save ခြင်း
        Design design = Design.builder()
                .title(title)
                .imageUrl(imageUrl)
                .reactionCount(0) // အသစ်မို့လို့ Default 0 ပေးသည်
                .build();

        Design savedDesign = designRepository.save(design);

        // ၆။ DTO ပြန်ထုတ်ပေးခြင်း
        return DesignResponseDto.builder()
                .id(savedDesign.getId())
                .title(savedDesign.getTitle())
                .imageUrl(savedDesign.getImageUrl())
                .reactionCount(savedDesign.getReactionCount())
                .isFavorited(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignResponseDto> getTrendingDesigns(String customerEmail) {
        User customer = customerEmail != null ? userRepository.findByEmail(customerEmail).orElse(null) : null;
        Set<Long> favoriteDesignIds = customer != null ?
                customer.getFavoriteDesigns().stream().map(Design::getId).collect(Collectors.toSet()) : Set.of();

        // React အများဆုံးကနေစီပြီး၊ တူရင် A-Z စီထားသော List ကိုယူသည်
        List<Design> designs = designRepository.findAllByOrderByReactionCountDescTitleAsc();

        return designs.stream().map(design -> DesignResponseDto.builder()
                .id(design.getId())
                .title(design.getTitle())
                .imageUrl(design.getImageUrl())
                .reactionCount(design.getReactionCount())
                .isFavorited(favoriteDesignIds.contains(design.getId()))
                .build()
        ).toList();
    }

    // ၂။ နှလုံးသားပုံ နှိပ်လိုက်လျှင် Like / Unlike လုပ်ပေးမည့် Logic (Toggle)
    @Override
    @Transactional
    public void toggleReactToDesign(Long designId, String customerEmail) {
        Design design = designRepository.findById(designId)
                .orElseThrow(() -> new RuntimeException("Design not found"));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getFavoriteDesigns().contains(design)) {
            customer.getFavoriteDesigns().remove(design);
            design.setReactionCount(Math.max(0, design.getReactionCount() - 1));
        } else {
            customer.getFavoriteDesigns().add(design);
            design.setReactionCount(design.getReactionCount() + 1);
        }

        designRepository.save(design);
        userRepository.save(customer);
    }

    // ၃။ Profile Page တွင် ပြသရန် မိမိ Favorite ပေးထားခဲ့သော Designs များ ယူခြင်း
    @Override
    @Transactional(readOnly = true)
    public List<DesignResponseDto> getCustomerFavorites(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customer.getFavoriteDesigns().stream().map(design -> DesignResponseDto.builder()
                .id(design.getId())
                .title(design.getTitle())
                .imageUrl(design.getImageUrl())
                .reactionCount(design.getReactionCount())
                .isFavorited(true)
                .build()
        ).toList();
    }
}
