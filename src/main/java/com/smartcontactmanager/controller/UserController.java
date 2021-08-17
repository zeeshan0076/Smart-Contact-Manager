package com.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smartcontactmanager.dao.ContactRepository;
import com.smartcontactmanager.dao.UserRepository;
import com.smartcontactmanager.entities.Contact;
import com.smartcontactmanager.entities.User;
import com.smartcontactmanager.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME "+userName);
		
		// get the user using username(Email)
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER "+user);
		
		model.addAttribute("user",user);
	}
	
	// dashboard home	
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {	
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal,HttpSession session) {
		try {
			
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		// pocessing and uploading file
		if (file.isEmpty()) {
			// if file is empty then try our message
			System.out.println("File is empty");
			contact.setImage("contact.png");
		}else {
			//upload the file to folder and update the name to contact
			contact.setImage(file.getOriginalFilename());
			
			File saveFile = new ClassPathResource("static/image").getFile();
			
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("image is uploaded !!");
		}
		
		contact.setUser(user);
		user.getContacts().add(contact);
		
		this.userRepository.save(user);
		
		System.out.println("DATA"+contact);
		System.out.println("Added to database");
		
		// message success....
		session.setAttribute("message", new Message("Your contact is added !! Add more..", "success"));
		
		}catch (Exception e) {
			System.out.println("Error"+e.getMessage());
			e.printStackTrace();
			
			// message error
		session.setAttribute("message", new Message("Something went wrong !! try again..", "danger"));
		
		}
		
		return "normal/add_contact_form";
	}
	
	// show contact page
	// per page = 4[n]
	// current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContact(@PathVariable("page") Integer page,Model m,Principal principal) {
		m.addAttribute("title", "show user contacts");
		//contact ki list ko bhejna hai
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 4);
		
		Page<Contact> contacts =this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);		
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contact";
	}
	
	// showing particular contact details.
	
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		System.out.println("CID "+cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);		
		Contact contact = contactOptional.get();
		
		//
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact",contact);	
			model.addAttribute("title",contact.getName());
		}
		return "normal/contact_detail";
	}
	
	// delete contact handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,HttpSession session,Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		User user = this.userRepository.getUserByUserName(principal.getName());
		
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
		System.out.println("DELETED");
		session.setAttribute("message", new Message("Contact deleted successfully...", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	// open update form handler
	
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid,Model m) {
		
		m.addAttribute("title","Update contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact",contact);
		
		return "normal/update_form";
	}
	
	// update contact handler
	
	@RequestMapping(value = "/process-update",method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,
			Model m,HttpSession session,Principal principal) {
		
		try {
			
			// old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				// file work..
				//rewrite
				//delete old photo
				
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();
				
				
				// update new photo
				
				File saveFile = new ClassPathResource("static/image").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated...", "success"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	// show profile
	@GetMapping("/profile")
	public String showProfile(Model m) {
		m.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
	// open setting handler
	@GetMapping("/settings")
	public String openSetting() {
		return "normal/settings";
	}
	
	// change password
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword,
			Principal principal,HttpSession session) {
		System.out.println("OLD PASSWORD "+oldPassword);
		System.out.println("NEW PASSWORD "+newPassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.passwordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			// change the password
			currentUser.setPassword(this.passwordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password changed successfully..!!", "success"));
			
		}else {
			//error...
			session.setAttribute("message", new Message("Please enter correct old password..!!", "danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
	
	
	
	
	
	
	
	
	
	
	

}
