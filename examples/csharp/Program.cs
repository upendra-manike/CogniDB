using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace SyntricDBDemo
{
    class Program
    {
        private static readonly HttpClient client = new HttpClient();
        private const string SyntricDBUrl = "http://localhost:8080/api/sql";

        static async Task Main(string[] args)
        {
            Console.WriteLine("=================================================");
            Console.WriteLine("💜 SyntricDB C# / .NET 8 Integration Demo");
            Console.WriteLine("=================================================");

            // 1. Create Table
            string createSql = @"
                CREATE TABLE dotnet_events (
                    id VARCHAR PRIMARY KEY,
                    event_type VARCHAR,
                    severity VARCHAR,
                    embedding FLOAT_VECTOR(128)
                );";
            string res1 = await ExecuteQueryAsync(createSql);
            Console.WriteLine($"✅ Create Table Response: {res1}");

            // 2. Insert Record
            string insertSql = @"
                INSERT INTO dotnet_events VALUES (
                    'evt_701',
                    'DatabaseConnectionTimeout',
                    'HIGH',
                    AI_EMBED('database connection pool timeout error failure')
                );";
            string res2 = await ExecuteQueryAsync(insertSql);
            Console.WriteLine($"✅ Insert Record Response: {res2}");

            // 3. Vector Similarity Search Query
            string searchSql = @"
                SELECT id, event_type, severity 
                FROM dotnet_events 
                WHERE embedding SIMILAR TO 'connection timeout error' 
                TOP 1;";
            string res3 = await ExecuteQueryAsync(searchSql);
            Console.WriteLine($"\n🔍 Vector Search Results:\n{res3}");

            Console.WriteLine("=================================================");
        }

        private static async Task<string> ExecuteQueryAsync(string sql)
        {
            var json = JsonSerializer.Serialize(new { sql = sql });
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(SyntricDBUrl, content);
            return await response.Content.ReadAsStringAsync();
        }
    }
}
