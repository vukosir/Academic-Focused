using CMCS.Models;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;

namespace CMCS.Services
{
    public class ClaimService : IClaimService
    {
        private List<MonthlyClaim> _claims;
        private readonly string _dataFile = "Data/claims.json";

        public ClaimService()
        {
            _claims = LoadClaimsFromFile();
        }

        public IEnumerable<MonthlyClaim> GetAllClaims()
        {
            return _claims.OrderByDescending(c => c.SubmissionDate).ToList();
        }

        public MonthlyClaim? GetClaimById(int id)
        {
            return _claims.FirstOrDefault(c => c.Id == id);
        }

        public void AddClaim(MonthlyClaim claim)
        {
            claim.Id = _claims.Count > 0 ? _claims.Max(c => c.Id) + 1 : 1;
            _claims.Add(claim);
            SaveChanges();
        }

        public void UpdateClaim(MonthlyClaim claim)
        {
            var existingClaim = GetClaimById(claim.Id);
            if (existingClaim != null)
            {
                var index = _claims.IndexOf(existingClaim);
                _claims[index] = claim;
                SaveChanges();
            }
        }

        public void DeleteClaim(int id)
        {
            var claim = GetClaimById(id);
            if (claim != null)
            {
                _claims.Remove(claim);
                SaveChanges();
            }
        }

        public IEnumerable<MonthlyClaim> GetPendingClaims()
        {
            return _claims.Where(c => c.Status == "Pending").OrderBy(c => c.SubmissionDate).ToList();
        }

        public IEnumerable<MonthlyClaim> GetClaimsByLecturerId(string lecturerId)
        {
            return _claims.Where(c => c.LecturerId == lecturerId).OrderByDescending(c => c.SubmissionDate).ToList();
        }

        public void SaveChanges()
        {
            try
            {
                var directory = Path.GetDirectoryName(_dataFile);
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory!);
                }

                var options = new JsonSerializerOptions { WriteIndented = true };
                var json = JsonSerializer.Serialize(_claims, options);
                File.WriteAllText(_dataFile, json);
            }
            catch
            {
                Console.WriteLine("Error saving claims");
            }
        }

        private List<MonthlyClaim> LoadClaimsFromFile()
        {
            try
            {
                if (File.Exists(_dataFile))
                {
                    var json = File.ReadAllText(_dataFile);
                    var claims = JsonSerializer.Deserialize<List<MonthlyClaim>>(json) ?? new List<MonthlyClaim>();
                    if (claims.Any()) return claims;
                }
            }
            catch
            {
                Console.WriteLine("Error loading claims from file");
            }

            // Return comprehensive sample data for prototype
            var currentDate = DateTime.Now;
            return new List<MonthlyClaim>
            {
                // PENDING CLAIMS - For Review page
                new MonthlyClaim {
                    Id = 1,
                    LecturerId = "L001",
                    FullName = "Dr. John Smith",
                    Department = "Computer Science",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1),
                    HoursWorked = 45,
                    HourlyRate = 550,
                    Status = "Pending",
                    SubmissionDate = currentDate.AddDays(-2),
                    Notes = "Advanced Programming lectures and lab supervision",
                    FileName = "timesheet_october.pdf",
                    FileContent = System.Text.Encoding.UTF8.GetBytes("Sample PDF content"),
                    FileContentType = "application/pdf"
                },
                new MonthlyClaim {
                    Id = 2,
                    LecturerId = "L002",
                    FullName = "Prof. Sarah Brown",
                    Department = "Mathematics",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1),
                    HoursWorked = 38,
                    HourlyRate = 520,
                    Status = "Pending",
                    SubmissionDate = currentDate.AddDays(-1),
                    Notes = "Calculus II and Linear Algebra tutorials",
                    FileName = "teaching_hours.docx",
                    FileContent = System.Text.Encoding.UTF8.GetBytes("Sample DOCX content"),
                    FileContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                },
                new MonthlyClaim {
                    Id = 3,
                    LecturerId = "L003",
                    FullName = "Dr. David Green",
                    Department = "Physics",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1),
                    HoursWorked = 42,
                    HourlyRate = 580,
                    Status = "Pending",
                    SubmissionDate = currentDate.AddDays(-3),
                    Notes = "Quantum mechanics practical sessions and exam marking"
                },

                // APPROVED CLAIMS - For tracking demonstration
                new MonthlyClaim {
                    Id = 4,
                    LecturerId = "L004",
                    FullName = "Dr. Emily Davis",
                    Department = "Computer Science",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-1),
                    HoursWorked = 40,
                    HourlyRate = 560,
                    Status = "Approved",
                    SubmissionDate = currentDate.AddDays(-15),
                    ApprovalDate = currentDate.AddDays(-10),
                    ApprovedBy = "Coordinator",
                    Notes = "Database systems and web development workshops",
                    FileName = "september_timesheet.xlsx",
                    FileContent = System.Text.Encoding.UTF8.GetBytes("Sample XLSX content"),
                    FileContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                },
                new MonthlyClaim {
                    Id = 5,
                    LecturerId = "L005",
                    FullName = "Prof. Michael Wilson",
                    Department = "Engineering",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-1),
                    HoursWorked = 35,
                    HourlyRate = 600,
                    Status = "Approved",
                    SubmissionDate = currentDate.AddDays(-18),
                    ApprovalDate = currentDate.AddDays(-12),
                    ApprovedBy = "Manager",
                    Notes = "Engineering mathematics and project supervision"
                },
                new MonthlyClaim {
                    Id = 6,
                    LecturerId = "L001",
                    FullName = "Dr. John Smith",
                    Department = "Computer Science",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-2),
                    HoursWorked = 48,
                    HourlyRate = 550,
                    Status = "Approved",
                    SubmissionDate = currentDate.AddDays(-45),
                    ApprovalDate = currentDate.AddDays(-40),
                    ApprovedBy = "Manager",
                    Notes = "Summer intensive programming bootcamp",
                    FileName = "bootcamp_hours.pdf",
                    FileContent = System.Text.Encoding.UTF8.GetBytes("Sample PDF content"),
                    FileContentType = "application/pdf"
                },

                // REJECTED CLAIMS - For tracking demonstration
                new MonthlyClaim {
                    Id = 7,
                    LecturerId = "L006",
                    FullName = "Dr. Lisa Johnson",
                    Department = "Biology",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-1),
                    HoursWorked = 50,
                    HourlyRate = 480,
                    Status = "Rejected",
                    SubmissionDate = currentDate.AddDays(-20),
                    ApprovalDate = currentDate.AddDays(-14),
                    ApprovedBy = "Coordinator",
                    RejectionReason = "Hours claimed exceed maximum allowed per month. Please provide justification for overtime.",
                    Notes = "Extended lab sessions and research supervision"
                },
                new MonthlyClaim {
                    Id = 8,
                    LecturerId = "L002",
                    FullName = "Prof. Sarah Brown",
                    Department = "Mathematics",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-2),
                    HoursWorked = 32,
                    HourlyRate = 520,
                    Status = "Rejected",
                    SubmissionDate = currentDate.AddDays(-50),
                    ApprovalDate = currentDate.AddDays(-45),
                    ApprovedBy = "Coordinator",
                    RejectionReason = "Missing supporting documentation for claimed hours",
                    Notes = "Statistics and probability theory"
                },
                new MonthlyClaim {
                    Id = 9,
                    LecturerId = "L007",
                    FullName = "Dr. Robert Taylor",
                    Department = "Chemistry",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-1),
                    HoursWorked = 55,
                    HourlyRate = 510,
                    Status = "Rejected",
                    SubmissionDate = currentDate.AddDays(-25),
                    ApprovalDate = currentDate.AddDays(-18),
                    ApprovedBy = "Manager",
                    RejectionReason = "Hourly rate exceeds department maximum for this role. Please review contract terms.",
                    Notes = "Organic chemistry labs and research mentoring"
                },

                // ADDITIONAL APPROVED CLAIMS
                new MonthlyClaim {
                    Id = 10,
                    LecturerId = "L008",
                    FullName = "Prof. Maria Garcia",
                    Department = "Computer Science",
                    ClaimMonth = new DateTime(currentDate.Year, currentDate.Month, 1).AddMonths(-1),
                    HoursWorked = 36,
                    HourlyRate = 575,
                    Status = "Approved",
                    SubmissionDate = currentDate.AddDays(-22),
                    ApprovalDate = currentDate.AddDays(-16),
                    ApprovedBy = "Coordinator",
                    Notes = "Artificial Intelligence and Machine Learning courses"
                }
            };
        }
    }
}