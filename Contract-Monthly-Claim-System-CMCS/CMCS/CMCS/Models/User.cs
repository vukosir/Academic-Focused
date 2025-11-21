using Microsoft.AspNetCore.Identity;
using System.ComponentModel.DataAnnotations;

namespace CMCS.Models
{
    public class User : IdentityUser
    {
        [Required]
        [Display(Name = "Full Name")]
        public string FullName { get; set; } = "";

        [Required]
        public string Role { get; set; } = "";

        public string? Department { get; set; }

        [Display(Name = "Employee/Lecturer ID")]
        public string? EmployeeId { get; set; }

        public DateTime CreatedAt { get; set; } = DateTime.Now;
    }

    public static class UserRoles
    {
        public const string Lecturer = "Lecturer";
        public const string Coordinator = "Coordinator";
        public const string Manager = "Manager";
        public const string HR = "HR";
        public const string Admin = "Admin";
    }

    public class LoginViewModel
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = "";

        [Required]
        [DataType(DataType.Password)]
        public string Password { get; set; } = "";

        [Display(Name = "Remember me?")]
        public bool RememberMe { get; set; }
    }

    public class RegisterViewModel
    {
        [Required]
        [EmailAddress]
        [Display(Name = "Email")]
        public string Email { get; set; } = "";

        [Required]
        [StringLength(100, ErrorMessage = "The {0} must be at least {2} and at max {1} characters long.", MinimumLength = 6)]
        [DataType(DataType.Password)]
        [Display(Name = "Password")]
        public string Password { get; set; } = "";

        [DataType(DataType.Password)]
        [Display(Name = "Confirm password")]
        [Compare("Password", ErrorMessage = "The password and confirmation password do not match.")]
        public string ConfirmPassword { get; set; } = "";

        [Required]
        [Display(Name = "Full Name")]
        public string FullName { get; set; } = "";

        [Required]
        [Display(Name = "Role")]
        public string Role { get; set; } = "";

        [Display(Name = "Employee/Lecturer ID")]
        public string? EmployeeId { get; set; }

        [Display(Name = "Department")]
        public string? Department { get; set; }
    }
}
