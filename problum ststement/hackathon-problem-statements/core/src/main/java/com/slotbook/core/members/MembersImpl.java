package com.slotbook.core.members;

import com.slotbook.contract.MemberView;
import com.slotbook.contract.Members;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MembersImpl implements Members {

    private final MemberRepository members;

    public MembersImpl(MemberRepository members) {
        this.members = members;
    }

    @Override
    public Optional<MemberView> find(long memberId) {
        return members.findById(memberId).map(Member::view);
    }
}
