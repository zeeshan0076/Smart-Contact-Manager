package com.smartcontactmanager.dao;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcontactmanager.entities.Contact;
import com.smartcontactmanager.entities.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
	// pagination
	
	@Query("from Contact as c where c.user.id =:userId")
	//current page-page
	//contact per page - 4
	public Page<Contact> findContactsByUser(@Param("userId")int userId, Pageable pePageable);
	
	// search
	public List<Contact> findByNameContainingAndUser(String name,User user);

}
