with Ada.Text_IO; use Ada.Text_IO;

procedure Main is
   Result : Integer := 0;
begin
   Put_Line ("Running the AdaLang Analyzer example");

   --  These constructs are intentional so the external analyzer has findings
   --  to publish in SonarQube.
   Result := 10 / 0;
   goto Finished;
   raise Program_Error;

   <<Finished>>
   Put_Line (Integer'Image (Result));
end Main;
