using System;
using System.ComponentModel.DataAnnotations;

namespace CMCS.Models
{
    public class MonthlyClaim
    {
        public int Id { get; set; }

        [Required(ErrorMessage = "Lecturer ID is required")]
        [Display(Name = "Lecturer ID")]
        [StringLength(20, ErrorMessage = "Lecturer ID cannot exceed 20 characters")]
        public string LecturerId { get; set; } = "";

        [Required(ErrorMessage = "Full name is required")]
        [Display(Name = "Full Name")]
        [StringLength(100, ErrorMessage = "Full name cannot exceed 100 characters")]
        public string FullName { get; set; } = "";

        [Required(ErrorMessage = "Department is required")]
        [StringLength(50, ErrorMessage = "Department cannot exceed 50 characters")]
        public string Department { get; set; } = "";

        [Required(ErrorMessage = "Claim month is required")]
        [Display(Name = "Claim Month")]
        [DataType(DataType.Date)]
        public DateTime ClaimMonth { get; set; } = new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1);

        [Required(ErrorMessage = "Hours worked is required")]
        [Range(0.1, 1000, ErrorMessage = "Hours worked must be between 0.1 and 1000")]
        [Display(Name = "Hours Worked")]
        public double HoursWorked { get; set; } = 0;

        [Required(ErrorMessage = "Hourly rate is required")]
        [Range(0.1, 10000, ErrorMessage = "Hourly rate must be between 0.1 and 10,000")]
        [Display(Name = "Hourly Rate")]
        public double HourlyRate { get; set; } = 0;

        [Display(Name = "Total Amount")]
        public double TotalAmount => HoursWorked * HourlyRate;

        public string Status { get; set; } = "Pending";

        [Display(Name = "Additional Notes")]
        [StringLength(500, ErrorMessage = "Notes cannot exceed 500 characters")]
        public string Notes { get; set; } = "";

        [Display(Name = "Supporting Document")]
        public string? FileName { get; set; }

        public byte[]? FileContent { get; set; }

        public string? FileContentType { get; set; }

        public DateTime SubmissionDate { get; set; } = DateTime.Now;
        public string? ApprovedBy { get; set; }
        public DateTime? ApprovalDate { get; set; }
        public string? RejectionReason { get; set; }

        // Helper properties
        public bool IsPending => Status == "Pending";
        public bool IsApproved => Status == "Approved";
        public bool IsRejected => Status == "Rejected";

        public string StatusColor => Status switch
        {
            "Approved" => "success",
            "Rejected" => "danger",
            "Pending" => "warning",
            _ => "secondary"
        };
    }
}
