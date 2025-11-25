package org.ncsu.settleup.membershipservice.controller;

import org.ncsu.settleup.membershipservice.model.CategoryEntity;
import org.ncsu.settleup.membershipservice.model.GroupEntity;
import org.ncsu.settleup.membershipservice.model.MemberEntity;
import org.ncsu.settleup.membershipservice.repo.CategoryRepository;
import org.ncsu.settleup.membershipservice.repo.GroupRepository;
import org.ncsu.settleup.membershipservice.repo.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

/**
 * REST controller for managing groups, members and categories.  The API
 * methods defined here closely follow the endpoints designed in
 * Assignment 1.  Swagger/OpenAPI documentation is generated
 * automatically by springdoc.
 */
@RestController
@RequestMapping("/groups")
public class MembershipController {
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private static final String GROUP_NOT_FOUND_MESSAGE = "Group not found";
    public MembershipController(GroupRepository groupRepository,
                                MemberRepository memberRepository,
                                CategoryRepository categoryRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * List all groups.
     *
     * @return list of groups
     */
    @GetMapping
    @Operation(summary = "List all groups")
    public List<GroupEntity> getGroups() {
        return groupRepository.findAll();
    }

    /**
     * Retrieve a single group by its identifier.  Returns 404 if not found.
     */
    @GetMapping("/{groupId}")
    @Operation(summary = "Get a group by ID")
    public ResponseEntity<GroupEntity> getGroup(@PathVariable Long groupId) {
        return groupRepository.findById(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Create a new group.
     *
     * @param request request body containing the group's name and base currency
     * @return the persisted group
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new group")
    public GroupEntity createGroup(@RequestBody GroupCreateRequest request) {
        GroupEntity group = new GroupEntity(request.name(), request.baseCurrency());
        return groupRepository.save(group);
    }

    /**
     * Update an existing group.  Allows changing the name and base currency.
     *
     * @param groupId group identifier
     * @param request update payload
     * @return the updated group
     */
    @PutMapping("/{groupId}")
    @Operation(summary = "Update an existing group")
    public GroupEntity updateGroup(@PathVariable Long groupId,
                                   @RequestBody GroupUpdateRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND_MESSAGE));
        if (request.name() != null) {
            group.setName(request.name());
        }
        if (request.baseCurrency() != null) {
            group.setBaseCurrency(request.baseCurrency());
        }
        return groupRepository.save(group);
    }

    /**
     * Delete a group and all associated members and categories.
     */
    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group and its members/categories")
    public ResponseEntity<String> deleteGroup(@PathVariable Long groupId) {
        return groupRepository.findById(groupId)
                .map(g -> {
                    groupRepository.delete(g);
                    return ResponseEntity.ok("Group deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(GROUP_NOT_FOUND_MESSAGE));
    }

    /**
     * List all members of the given group.
     *
     * @param groupId group identifier
     * @return list of members
     */
    @GetMapping("/{groupId}/members")
    @Operation(summary = "List members of a group")
    public ResponseEntity<List<MemberEntity>> getGroupMembers(@PathVariable Long groupId) {
        return groupRepository.findById(groupId)
                .map(group -> {
                    List<MemberEntity> members = memberRepository.findAll().stream()
                            .filter(m -> m.getGroup().getId().equals(groupId))
                            .toList();
                    return ResponseEntity.ok(members);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Add a new member to the given group.
     *
     * @param groupId group identifier
     * @param request request body containing the member's email and role
     * @return the persisted member
     */
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a member to a group")
    public MemberEntity addMember(@PathVariable Long groupId,
                                  @RequestBody MemberCreateRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND_MESSAGE));
        MemberEntity member = new MemberEntity(request.email(), request.role(), group);
        return memberRepository.save(member);
    }

    /**
     * Update an existing member's details.  Only email and role can be changed.
     */
    @PutMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Update a member's email or role")
    public MemberEntity updateMember(@PathVariable Long groupId,
                                     @PathVariable Long memberId,
                                     @RequestBody MemberUpdateRequest request) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        if (!member.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Member does not belong to group");
        }
        if (request.email() != null) {
            member.setEmail(request.email());
        }
        if (request.role() != null) {
            member.setRole(request.role());
        }
        return memberRepository.save(member);
    }

    /**
     * Remove a member from a group.
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Delete a member from a group")
    public ResponseEntity<String> deleteMember(@PathVariable Long groupId,
                                               @PathVariable Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> {
                    if (!member.getGroup().getId().equals(groupId)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Member does not belong to this group");
                    }
                    memberRepository.delete(member);
                    return ResponseEntity.ok("Member deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found"));
    }

    /**
     * List categories associated with the given group.
     */
    @GetMapping("/{groupId}/categories")
    @Operation(summary = "List categories of a group")
    public ResponseEntity<List<CategoryEntity>> getGroupCategories(@PathVariable Long groupId) {
        return groupRepository.findById(groupId)
                .map(g -> {
                    List<CategoryEntity> cats = categoryRepository.findAll().stream()
                            .filter(c -> c.getGroup().getId().equals(groupId))
                            .toList();
                    return ResponseEntity.ok(cats);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Add a new category to the given group.
     */
    @PostMapping("/{groupId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a category to a group")
    public CategoryEntity addCategory(@PathVariable Long groupId,
                                      @RequestBody CategoryCreateRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND_MESSAGE));
        CategoryEntity category = new CategoryEntity(request.name(), group);
        return categoryRepository.save(category);
    }

    /**
     * Update an existing category's name.
     */
    @PutMapping("/{groupId}/categories/{categoryId}")
    @Operation(summary = "Update a category's name")
    public CategoryEntity updateCategory(@PathVariable Long groupId,
                                         @PathVariable Long categoryId,
                                         @RequestBody CategoryUpdateRequest request) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!category.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Category does not belong to group");
        }
        if (request.name() != null) {
            category.setName(request.name());
        }
        return categoryRepository.save(category);
    }

    /**
     * Remove a category from a group.
     */
    @DeleteMapping("/{groupId}/categories/{categoryId}")
    @Operation(summary = "Delete a category from a group")
    public ResponseEntity<String> deleteCategory(@PathVariable Long groupId,
                                                 @PathVariable Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(cat -> {
                    if (!cat.getGroup().getId().equals(groupId)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Category does not belong to this group");
                    }
                    categoryRepository.delete(cat);
                    return ResponseEntity.ok("Category deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found"));
    }

    /**
     * Request body for creating a group.  Declared as a record to
     * leverage compact syntax and generated getters.
     */
    public static record GroupCreateRequest(String name, String baseCurrency) {
    }

    /**
     * Request body for updating a group.  Any field may be null to leave unchanged.
     */
    public static record GroupUpdateRequest(String name, String baseCurrency) {
    }

    /**
     * Request body for creating a member.
     */
    public static record MemberCreateRequest(String email, String role) {
    }

    /**
     * Request body for updating a member.
     */
    public static record MemberUpdateRequest(String email, String role) {
    }

    /**
     * Request body for creating a category.
     */
    public static record CategoryCreateRequest(String name) {
    }

    /**
     * Request body for updating a category.
     */
    public static record CategoryUpdateRequest(String name) {
    }
}
