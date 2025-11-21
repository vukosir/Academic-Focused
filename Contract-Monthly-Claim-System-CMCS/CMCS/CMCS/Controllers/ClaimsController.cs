using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using System.Collections.Generic;
using System.Linq;
using System.IO;
using CMCS.Models;
using CMCS.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using System.Security.Claims;

namespace CMCS.Controllers
{
    [Authorize]
    public class ClaimsController : Controller
    {
        private readonly IClaimService _claimService;
        private readonly UserManager<User> _userManager;

        public ClaimsController(IClaimService claimService, UserManager<User> userManager)
        {
            _claimService = claimService;
            _userManager = userManager;
        }

        [HttpGet]
        [Authorize(Roles = UserRoles.Lecturer)]
        public IActionResult Submit()
        {
            var user = _userManager.GetUserAsync(User).Result;
            var claim = new MonthlyClaim();

            if (user != null)
            {
                claim.LecturerId = user.EmployeeId ?? "LEC" + user.Id.Substring(0, 4).ToUpper();
                claim.FullName = user.FullName;
                claim.Department = user.Department ?? "Computer Science";
            }

            return View(claim);
        }

        [HttpPost]
        [Authorize(Roles = UserRoles.Lecturer)]
        [ValidateAntiForgeryToken]
        public IActionResult Submit(MonthlyClaim claim, IFormFile? supportingDocument)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    var user = _userManager.GetUserAsync(User).Result;
                    if (user != null)
                    {
                        claim.LecturerId = user.EmployeeId ?? "LEC" + user.Id.Substring(0, 4).ToUpper();
                        claim.FullName = user.FullName;
                        claim.Department = user.Department ?? "Computer Science";
                    }

                    if (supportingDocument != null && supportingDocument.Length > 0)
                    {
                        var (isValid, errorMessage) = ValidateFile(supportingDocument);
                        if (!isValid)
                        {
                            ModelState.AddModelError("supportingDocument", errorMessage);
                            return View(claim);
                        }

                        if (!IsFileSafe(supportingDocument))
                        {
                            ModelState.AddModelError("supportingDocument", "File appears to be corrupted or malicious.");
                            return View(claim);
                        }

                        using var memoryStream = new MemoryStream();
                        supportingDocument.CopyTo(memoryStream);
                        claim.FileName = supportingDocument.FileName;
                        claim.FileContent = memoryStream.ToArray();
                        claim.FileContentType = supportingDocument.ContentType;
                    }

                    claim.Status = "Pending";
                    claim.SubmissionDate = DateTime.Now;

                    _claimService.AddClaim(claim);
                    TempData["SuccessMessage"] = $"Claim submitted successfully! Your claim ID is {claim.Id}.";
                    return RedirectToAction("Index", "Home");
                }
            }
            catch
            {
                ModelState.AddModelError("", "An unexpected error occurred while submitting your claim. Please try again.");
            }

            return View(claim);
        }

        [HttpGet]
        [Authorize(Roles = $"{UserRoles.Coordinator},{UserRoles.Manager}")]
        public IActionResult Review()
        {
            var pendingClaims = _claimService.GetPendingClaims();
            return View(pendingClaims);
        }

        [HttpPost]
        [Authorize(Roles = $"{UserRoles.Coordinator},{UserRoles.Manager}")]
        [ValidateAntiForgeryToken]
        public IActionResult Approve(int id)
        {
            try
            {
                var claim = _claimService.GetClaimById(id);
                if (claim != null)
                {
                    claim.Status = "Approved";
                    claim.ApprovalDate = DateTime.Now;
                    claim.ApprovedBy = User.Identity?.Name ?? "System";
                    _claimService.UpdateClaim(claim);

                    TempData["SuccessMessage"] = $"Claim {claim.LecturerId} from {claim.FullName} approved successfully!";
                }
                else
                {
                    TempData["ErrorMessage"] = "Claim not found.";
                }
            }
            catch
            {
                TempData["ErrorMessage"] = "An error occurred while approving the claim.";
            }

            return RedirectToAction(nameof(Review));
        }

        [HttpPost]
        [Authorize(Roles = $"{UserRoles.Coordinator},{UserRoles.Manager}")]
        [ValidateAntiForgeryToken]
        public IActionResult Reject(int id, string rejectionReason)
        {
            try
            {
                var claim = _claimService.GetClaimById(id);
                if (claim != null)
                {
                    if (string.IsNullOrWhiteSpace(rejectionReason))
                    {
                        TempData["ErrorMessage"] = "Rejection reason is required.";
                        return RedirectToAction(nameof(Review));
                    }

                    claim.Status = "Rejected";
                    claim.ApprovalDate = DateTime.Now;
                    claim.ApprovedBy = User.Identity?.Name ?? "System";
                    claim.RejectionReason = rejectionReason.Trim();
                    _claimService.UpdateClaim(claim);

                    TempData["SuccessMessage"] = $"Claim {claim.LecturerId} from {claim.FullName} has been rejected.";
                }
                else
                {
                    TempData["ErrorMessage"] = "Claim not found.";
                }
            }
            catch
            {
                TempData["ErrorMessage"] = "An error occurred while rejecting the claim.";
            }

            return RedirectToAction(nameof(Review));
        }

        [HttpGet]
        public IActionResult DownloadDocument(int id)
        {
            try
            {
                var claim = _claimService.GetClaimById(id);
                if (claim?.FileContent != null && claim.FileName != null)
                {
                    return File(claim.FileContent, claim.FileContentType ?? "application/octet-stream", claim.FileName);
                }

                TempData["ErrorMessage"] = "Document not found.";
            }
            catch
            {
                TempData["ErrorMessage"] = "An error occurred while downloading the document.";
            }

            return RedirectToAction(nameof(Review));
        }

        [HttpGet]
        [Authorize(Roles = $"{UserRoles.Coordinator},{UserRoles.Manager},{UserRoles.HR},{UserRoles.Admin}")]
        public IActionResult Manage()
        {
            var claims = _claimService.GetAllClaims();
            return View(claims);
        }

        [HttpGet]
        public IActionResult Track()
        {
            var user = _userManager.GetUserAsync(User).Result;
            IEnumerable<MonthlyClaim> claims;

            if (user?.Role == UserRoles.Lecturer)
            {
                claims = _claimService.GetClaimsByLecturerId(user.EmployeeId ?? "LEC" + user.Id.Substring(0, 4).ToUpper());
            }
            else
            {
                claims = _claimService.GetAllClaims();
            }

            var statistics = new
            {
                TotalClaims = claims.Count(),
                ApprovedClaims = claims.Count(c => c.Status == "Approved"),
                RejectedClaims = claims.Count(c => c.Status == "Rejected"),
                PendingClaims = claims.Count(c => c.Status == "Pending"),
                TotalAmount = claims.Sum(c => c.TotalAmount),
                ApprovedAmount = claims.Where(c => c.Status == "Approved").Sum(c => c.TotalAmount),
                PendingAmount = claims.Where(c => c.Status == "Pending").Sum(c => c.TotalAmount),
                RejectedAmount = claims.Where(c => c.Status == "Rejected").Sum(c => c.TotalAmount)
            };

            ViewBag.Statistics = statistics;
            return View(claims);
        }

        [HttpGet]
        public IActionResult Details(int id)
        {
            var claim = _claimService.GetClaimById(id);
            if (claim == null)
            {
                return NotFound();
            }

            var user = _userManager.GetUserAsync(User).Result;
            if (user?.Role == UserRoles.Lecturer && claim.LecturerId != user.EmployeeId)
            {
                return Forbid();
            }

            return PartialView("_ClaimDetails", claim);
        }

        [HttpPost]
        [Authorize(Roles = $"{UserRoles.Coordinator},{UserRoles.Manager},{UserRoles.HR},{UserRoles.Admin}")]
        [ValidateAntiForgeryToken]
        public IActionResult Delete(int id)
        {
            try
            {
                var claim = _claimService.GetClaimById(id);
                if (claim != null)
                {
                    _claimService.DeleteClaim(id);
                    TempData["SuccessMessage"] = $"Claim {claim.LecturerId} has been deleted.";
                }
                else
                {
                    TempData["ErrorMessage"] = "Claim not found.";
                }
            }
            catch
            {
                TempData["ErrorMessage"] = "An error occurred while deleting the claim.";
            }

            return RedirectToAction(nameof(Manage));
        }

        private (bool isValid, string errorMessage) ValidateFile(IFormFile file)
        {
            var allowedExtensions = new[] { ".pdf", ".docx", ".xlsx" };
            var fileExtension = Path.GetExtension(file.FileName).ToLower();

            if (!allowedExtensions.Contains(fileExtension))
            {
                return (false, "Only PDF, DOCX, and XLSX files are allowed.");
            }

            if (file.Length > 5 * 1024 * 1024)
            {
                return (false, "File size must be less than 5MB.");
            }

            if (file.Length == 0)
            {
                return (false, "File is empty.");
            }

            return (true, string.Empty);
        }

        private bool IsFileSafe(IFormFile file)
        {
            try
            {
                using var stream = file.OpenReadStream();
                byte[] fileHeader = new byte[8];
                stream.Read(fileHeader, 0, 8);

                var extension = Path.GetExtension(file.FileName).ToLower();

                if (extension == ".pdf" && !(fileHeader[0] == 0x25 && fileHeader[1] == 0x50 && fileHeader[2] == 0x44 && fileHeader[3] == 0x46))
                {
                    return false;
                }

                return true;
            }
            catch
            {
                return false;
            }
        }
    }
}
