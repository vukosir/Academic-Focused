using CMCS.Models;
using System.Collections.Generic;

namespace CMCS.Services
{
    public interface IClaimService
    {
        IEnumerable<MonthlyClaim> GetAllClaims();
        MonthlyClaim? GetClaimById(int id);
        void AddClaim(MonthlyClaim claim);
        void UpdateClaim(MonthlyClaim claim);
        void DeleteClaim(int id);
        IEnumerable<MonthlyClaim> GetPendingClaims();
        IEnumerable<MonthlyClaim> GetClaimsByLecturerId(string lecturerId);
        void SaveChanges();
    }
}
